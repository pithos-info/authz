# Pithos AuthZ Service

Combined authorization service for the Pithos platform. Covers two orthogonal authorization planes:

| Plane | Module group | What it controls |
|---|---|---|
| **Identity AuthZ** | `rbac/` | Users, groups, roles, permissions, API keys, and the `Account` tenant abstraction |
| **Monetization AuthZ** | `monetization/` | App catalog, features, journeys, workflows — the immutable metadata that governs what an account can execute and at what depth |

Both planes are deployed together in a single `authz-app` process and exposed via the same `authz-mcp` MCP server.

---

## Module structure

```
authz/
  pom.xml                    info.pithos.authz:info-pithos-authz  (authz parent)

  rbac/
    pom.xml                  info.pithos.rbac:info-pithos-rbac    (rbac sub-parent)
    rbac-model/              RBAC.proto + RBACService.proto
    rbac-service/            Service interfaces + relational implementations
    rbac-postgres/           Postgres Guice module — local dev
    rbac-cloudsql/           Cloud SQL Guice module — GCP

  monetization/
    pom.xml                  info.pithos.monetization:info-pithos-monetization  (monetization sub-parent)
    monetization-model/      Monetization.proto + MonetizationEvents.proto
    monetization-service/    Service interfaces + relational implementations
    monetization-postgres/   Postgres Guice module — local dev
    monetization-cloudsql/   Cloud SQL Guice module — GCP

  authz-app/                 Runnable assembly: REST + gRPC servers, both module groups wired together
  authz-mcp/                 MCP server exposing both RBAC and monetization authz context to AI agents
  client-api/                Bruno API collection
  scripts/                   Dev lifecycle scripts (migrate, seed, cleanup)
```

---

## Two authorization planes

### Identity AuthZ — `rbac/`

Role-based access control. Manages enterprises, users, groups, roles, permissions, and API keys. Also owns the **Account** abstraction — the unified tenant object covering both consumer (PERSONAL) and SaaS (TEAM) applications.

See [RBAC detail](#rbac-data-model) below.

### Monetization AuthZ — `monetization/`

Catalog of immutable platform entities. Defines what features exist, how they compose into journeys, and how journeys are sequenced into workflows at different depth levels. This catalog is the authoritative reference for billing (Ledger) and execution (Kestrel). No tier, SKU, or credit rate data lives here — those belong in Ledger.

See [Monetization detail](#monetization-data-model) below.

---

## authz-app

### Assembly

```
mvn package -pl authz-app -am
java -Dauthz.config=/path/to/config.yaml -jar authz-app/target/authz-app.jar
```

### Startup

```
Phase 1 — ApplicationContextImpl constructor     init() on all modules — no I/O
Phase 2 — appContext.start(30, SECONDS).join()   start() on all modules — connections + migrations
Phase 3 — grpcServer.start(); httpServer.start() — accept traffic
```

### Guice wiring

| Profile | Modules |
|---|---|
| Local | `PostgresRbacModule`, `PostgresMonetizationModule`, `BypassAuthModule` or `GcpIdentityOAuthModule`, `HashiCorpVaultModule`, `MinioBlobStorageModule`, `AuthZAppModule` |
| GCP | `CloudSqlRbacModule`, `CloudSqlMonetizationModule`, `GcpIdentityOAuthModule`, `GcpSecretManagerModule`, `GcsBlobStorageModule`, `AuthZAppModule` |

### YAML config

```yaml
server:
  httpPort: 8080
  grpcPort: 9090

bootstrapConfigs:
  serviceName: authz-service
  multiplier: 2

postgresConfigs:          { host: localhost, port: 5432, ... }
redisConfigs:             { host: localhost, port: 6379, ... }
keycloakOAuthConfigs:     { serverUrl: ..., realm: pithos, ... }
hashiCorpVaultConfigs:    { address: http://localhost:8200, ... }
minioBlobStorageConfigs:  { endpoint: http://localhost:9000, ... }
```

---

---

## Monetization data model

### Two proto files

#### `Monetization.proto` — catalog entities (`info.pithos.monetization.model`)

All four catalog entities are **immutable**. A change to any entity produces a new versioned record (`version + 1`, `parentXxxId` pointing back) — existing records are never updated in place. This makes journeyId / featureId references in events permanent, stable, cacheable snapshots.

`appId` is the catalog partition key (not `enterpriseId`) — the catalog is per-app, not per-enterprise.

| Message | Table | Notes |
|---|---|---|
| `App` | `app` | Root of the catalog; no description — never customer-facing |
| `Feature` | `feature` | API capability unit; `richardsonMaturityLevel` is the only semantic attribute |
| `Journey` | `journey` | User-facing business outcome; has `goal` and `outcomeStatement` |
| `Workflow` | `workflow` | One per journey per `depthLevel`; sequences features |
| `WorkflowFeature` | `workflowFeature` | Association binding a feature into a workflow at a `stepOrder` |

```proto
enum DepthLevel {
  DEPTH_LEVEL_UNSPECIFIED = 0;
  PRE_BUILT    = 1;    // fully managed, zero config
  CONFIGURABLE = 2;    // user tunes parameters
  COMPOSABLE   = 3;    // user assembles from parts
  CUSTOM       = 4;    // user brings their own implementation
}

message App {
  string id          = 1;
  string slug        = 2;
  string name        = 3;
  string ownerId     = 4;
  int32  version     = 5;
  string parentAppId = 6;
  int64  utcCreatedAt = 7;
  string createdBy   = 8;
}

message Feature {
  string id                      = 1;
  string appId                   = 2;
  string name                    = 3;
  int32  richardsonMaturityLevel = 4;
  int32  version                 = 5;
  string parentFeatureId         = 6;
  int64  utcCreatedAt            = 7;
  string createdBy               = 8;
}

message Journey {
  string id               = 1;
  string appId            = 2;
  string name             = 3;
  string displayName      = 4;
  string description      = 5;
  string goal             = 6;
  string outcomeStatement = 7;
  int32  version          = 8;
  string parentJourneyId  = 9;
  int64  utcCreatedAt     = 10;
  string createdBy        = 11;
}

message Workflow {
  string id               = 1;
  string appId            = 2;
  string journeyId        = 3;
  DepthLevel depthLevel   = 4;
  int32  version          = 5;
  string parentWorkflowId = 6;
  int64  utcCreatedAt     = 7;
  string createdBy        = 8;
}

message WorkflowFeature {
  string appId        = 1;
  string workflowId   = 2;
  string featureId    = 3;
  int32  stepOrder    = 4;
  int64  utcCreatedAt = 5;
  string createdBy    = 6;
}
```

#### `MonetizationService.proto` — public service API (`info.pithos.monetization.service`)

The public contract for REST and gRPC. Imports `Monetization.proto` and `google/protobuf/empty.proto`. Key design decisions:

- `Create*` requests all carry an optional `parentXxxId` field. Absent → `version = 1`. Present → auto-increment from the parent record.
- `WorkflowFeatureService.Remove` returns `google.protobuf.Empty`.
- `WorkflowService.ListByApp` **does not exist** — the usage pattern is App → Journeys → Workflows, so only `ListByJourney` is needed.
- `WorkflowService.Get` and `WorkflowService.ListByJourney` return enriched types rather than flat `Workflow`:

```proto
message WorkflowStep {
  int32   stepOrder = 1;
  Feature feature   = 2;
}

message WorkflowDetail {
  Workflow              workflow = 1;
  repeated WorkflowStep steps   = 2;
}

message WorkflowDetailList {
  repeated WorkflowDetail workflows = 1;
}
```

`WorkflowDetail` assembly happens in `WorkflowHandlers` via a private static `assemble()` helper that fetches `WorkflowFeature` associations and resolves each feature in parallel using `CompletableFuture.allOf`. In steady state all sub-components are cache hits, so assembly involves zero DB calls.

#### `MonetizationEvents.proto` — event bus (`info.pithos.monetization.events`)

`JourneyEvent` is the only event emitted by Pithos. It is flat — all three catalog references (`journeyId`, `workflowId`, `featureId`) point to immutable records, so no snapshot is needed.

`CreditEvent` is **not** emitted here — Ledger emits it upon receiving a `JourneyEvent`.

```proto
enum JourneyOperationType {
  JOURNEY_OPERATION_TYPE_UNSPECIFIED = 0;
  STARTED      = 1;
  CHECKPOINT   = 2;
  COMPLETED    = 3;
  FAILED       = 4;
  CANCELLED    = 5;
  UNAUTHORIZED = 6;
}

message JourneyEvent {
  string               eventId            = 1;
  string               accountId          = 2;   // RBAC Account (PERSONAL or TEAM)
  string               userId             = 3;
  string               journeyId          = 4;
  string               workflowId         = 5;
  string               featureId          = 6;
  DepthLevel           depthLevel         = 7;
  JourneyOperationType operationType      = 8;
  string               journeyExecutionId = 9;
  string               requestId          = 10;
  int64                utcCreatedAt       = 11;
  int64                soxSequenceNumber  = 12;  // monotonic; SOX compliance
}
```

### Immutable entity design

Catalog entities extend `ProtoBufImmutableService<T>` (not `ProtoBufCrudService`). The interface provides only `create`, `create(parentId)`, and `get` — no `update`, no `delete`.

**Versioning:** calling `create(rc, entity, parentId)` fetches the parent, calls `withParent(parent, entityBase)` (overridden per service to set `version = parent.version + 1` and `parentXxxId = parent.getId()`), and persists the result. Absent `parentId` → `version = 1`.

Schema consequences:
- No `deleted` column — no soft-delete
- No `utcModifiedAt` — records are write-once
- No `updatedBy` — same reason
- `slug` on `App` carries a `UNIQUE` constraint

All four catalog entities use the read-through write-back list cache (`ProtoBufListCache`) since they are read frequently and change rarely.

### Monetization DDL conventions

Same as RBAC: quoted camelCase identifiers, `UUID PRIMARY KEY DEFAULT gen_random_uuid()`, `ON DELETE RESTRICT`, Liquibase XML changesets.

| Changeset | Table | Notes |
|---|---|---|
| `V001__create_app.xml` | `app` | Immutable; `slug UNIQUE` |
| `V002__create_feature.xml` | `feature` | Immutable |
| `V003__create_journey.xml` | `journey` | Immutable; `description`, `goal`, `outcomeStatement` as `TEXT` |
| `V004__create_workflow.xml` | `workflow` | Immutable; `depthLevel VARCHAR(50)` |
| `V005__create_workflow_feature.xml` | `workflowFeature` | Association; composite PK `(workflowId, featureId)` |

---

---

## RBAC data model

All entities are protobuf messages defined in `RBAC.proto`. The schema has two kinds of message:

**Entity messages** — have a surrogate `id` (UUID), `utcCreatedAt`, `utcModifiedAt`, `deleted` (soft-delete flag).

| Message | Table | Notes |
|---|---|---|
| `Enterprise` | `enterprise` | SaaS tenant root |
| `User` | `user` | IdP-authenticated; no passwords |
| `Group` | `group` | Named collection of users within an enterprise |
| `Role` | `role` | Built-in or custom, scoped to an enterprise |
| `ApiKey` | `apiKey` | Hard-deleted on revoke; no soft delete |
| `Account` | `account` | Unified tenant for consumer (`PERSONAL`) and SaaS (`TEAM`) apps; see Account model section |

**Association messages** — no surrogate `id`; composite primary key.

| Message | Table | Composite PK |
|---|---|---|
| `GroupMember` | `groupMember` | `(enterpriseId, groupId, userId)` |
| `UserRole` | `userRole` | `(enterpriseId, userId, roleId)` |
| `GroupRole` | `groupRole` | `(enterpriseId, groupId, roleId)` |
| `RolePermission` | `rolePermission` | `(enterpriseId, roleId, permission)` |
| `AccountUser` | `accountUser` | `(accountId, userId)` |

`enterpriseId` is the leading PK component on enterprise-scoped association tables — this doubles as the partition key for future distributed storage backends, and allows enterprise-scoped range scans without a join to the parent entity. `AccountUser` uses `(accountId, userId)` because accounts can exist independently of an enterprise (PERSONAL type).

### Naming conventions

- String fields named `"id"` or ending with `"Id"` are UUID columns. `ProtoBufStatement` and `ProtoBufRelationalClient` auto-convert these to `java.util.UUID` for JDBC.
- `permission` is plain `TEXT` — it does not end with `"Id"` and is passed as a `String`.
- `grantedById` on `UserRole` follows the `*Id` convention so it maps to UUID correctly.

### No `ON DELETE CASCADE`

Foreign keys use `ON DELETE RESTRICT`. Cascading deletes at the DB level are unpredictable at scale and incompatible with soft deletes. Offboarding is handled by explicit deletion scripts that run in dependency order.

---

## Account model

`Account` is the primary scoping unit for apps built on top of Pithos. It unifies two business models that previously required separate identity architectures:

| Account type | Business model | Who owns it | Linked to |
|---|---|---|---|
| `PERSONAL` | Consumer / B2C | A single user | `ownerId` = RBAC `userId`; `enterpriseId` is null |
| `TEAM` | SaaS / B2B | An organisation | `enterpriseId` = RBAC `Enterprise`; `ownerId` is null |

Downstream services (e.g. Kestrel ARA) scope their data to `accountId` rather than choosing between user-centric and enterprise-centric models. This decouples the domain model from the tenancy strategy.

### One-step onboarding — `CreateAccountRequest`

`CreateAccountRequest` is the single entry point for both consumer signup and SaaS tenant provisioning. All three objects are created atomically in one RPC call so there is no partial-failure window.

**Consumer signup** (`type = PERSONAL`):
```json
{
  "name": "Jane Doe",
  "type": "PERSONAL",
  "user": {
    "email": "jane@example.com",
    "externalId": "google-sub-12345",
    "idpProvider": "google",
    "displayName": "Jane Doe"
  }
}
```
Server creates: `User` (with `user.enterpriseId` left unset — the handler fills it), then `Account` with `ownerId = user.id`.

**SaaS tenant signup** (`type = TEAM`):
```json
{
  "name": "Acme Corp",
  "type": "TEAM",
  "user": {
    "email": "admin@acme.com",
    "externalId": "google-sub-67890",
    "idpProvider": "google",
    "displayName": "Acme Admin"
  },
  "enterprise": {
    "slug": "acme-corp",
    "name": "Acme Corp",
    "domain": "acme.com"
  }
}
```
Server creates: `Enterprise`, then `User` (with `user.enterpriseId = enterprise.id`), then `Account` with `enterpriseId = enterprise.id`.

In both cases the caller becomes the account `OWNER` via an `AccountUser` row inserted in the same transaction.

### DB-level type invariant

The `account` table carries a `CHECK` constraint that enforces the PERSONAL/TEAM invariant at the database layer:

```sql
CONSTRAINT "account_type_check" CHECK (
    (type = 'PERSONAL' AND "ownerId" IS NOT NULL AND "enterpriseId" IS NULL) OR
    (type = 'TEAM'     AND "enterpriseId" IS NOT NULL AND "ownerId" IS NULL)
)
```

Exactly one of `ownerId` / `enterpriseId` is set; the other is always null. This makes the type field redundant as a discriminator but keeps it explicit for readability.

### `AccountUser` — membership

`AccountUser` links users to accounts with an explicit `role`:

| Role | Meaning |
|---|---|
| `OWNER` | Full control; set for the user created during `CreateAccount` |
| `MEMBER` | Standard access; set when adding additional members via `AccountUserService.Add` |

`AccountUser` has no `deleted` column — membership rows are hard-deleted when a member is removed. The `OWNER` row is inserted as part of the `CreateAccount` transaction and cannot be removed without transferring ownership first (enforced at the service layer).

---

## Two proto files, two boundaries

### `RBAC.proto` — data model (`info.pithos.rbac.model`)

Internal representation. Used by service interfaces, relational clients, and the cache layer. Never exposed directly over any protocol.

### `RBACService.proto` — public service API (`info.pithos.rbac.service`)

The canonical public contract for REST and gRPC. Defines its own request/response message types — no `Rbac.*` types appear here. Handler code maps between the two layers using `ProtoBufMapper`.

Key design decisions in the service API types:

- Response types are named after the domain object (`Enterprise`, `User`, `Group`, `Role`) without a `Response` suffix — these are canonical API types, not DTOs.
- `Role` carries `repeated string permissions` — the effective permission strings.
- `Group` carries `repeated Role roles` — the roles assigned to the group, each with permissions.
- `Enterprise` carries `repeated Group groups` and `repeated Role roles` — the full RBAC policy graph for the tenant, excluding user identity.
- `User` carries `repeated Group groups` and `repeated Role roles` — the user's complete RBAC footprint.
- `ApiKey.keyHash` is not present in `ApiKey` response type — the hash never leaves the server.
- Fields like `deleted`, `externalId`, `idpProvider` exist in `Rbac.*` data types but are absent from service API types; `ProtoBufMapper` drops them automatically during the scalar copy pass.

### `ProtoBufMapper` — data model → service API

`ProtoBufMapper.map(sourceProto, targetBuilder)` copies scalar fields by name match (same field name, same proto type, same repeated cardinality). Message-typed nested fields (`groups`, `roles`, `permissions`) must be populated separately by the handler after the scalar pass, using the cache-backed list service APIs.

```java
// Scalar fields copied automatically
service.Enterprise base = ProtoBufMapper.map(dataEnterprise, service.Enterprise.newBuilder());

// Nested collections assembled explicitly from list services
service.Enterprise full = base.toBuilder()
    .addAllRoles(buildApiRoles(rc, injector))
    .addAllGroups(buildApiGroups(rc, injector))
    .build();
```

---

## Service design

### Authorization

`RelationalEnterpriseService` enforces a system-tenant admin guard on all mutating and listing operations (`create`, `update`, `delete`, `list`):

1. `authEnterpriseId(rc)` must equal `"1"` (the system enterprise) → `403 FORBIDDEN` otherwise
2. `UserRoleService.hasRole(rc, "1")` — caller must hold the admin role (id `"1"`) in the system enterprise → `403 FORBIDDEN` otherwise

`get` is intentionally left unguarded — any authenticated user can fetch an enterprise by ID.

`UserRoleService` is constructed before `RelationalEnterpriseService` in both DB modules and injected via the constructor so the check can be applied within the service layer without touching handlers.

---

### One service, one primary protobuf type

Each service owns one protobuf message type and returns that type from all reads and writes:

**RBAC services:**
- `EnterpriseService` → `Rbac.Enterprise`
- `UserService` → `Rbac.User`
- `GroupService` → `Rbac.Group`
- `RoleService` → `Rbac.Role`
- `ApiKeyService` → `Rbac.ApiKey`
- `AccountService` → `Rbac.Account`
- `GroupMemberService` → `Rbac.GroupMember`
- `UserRoleService` → `Rbac.UserRole`
- `GroupRoleService` → `Rbac.GroupRole`
- `RolePermissionService` → `Rbac.RolePermission`
- `AccountUserService` → `Rbac.AccountUser`

**Monetization services:**
- `AppService` → `Monetization.App`
- `FeatureService` → `Monetization.Feature`
- `JourneyService` → `Monetization.Journey`
- `WorkflowService` → `Monetization.Workflow`
- `WorkflowFeatureService` → `Monetization.WorkflowFeature`

### Entity services (`ProtoBufCrudService` / `ProtoBufImmutableService`)

**Mutable entities** (RBAC) extend `ProtoBufCrudService<T>`, which implements `CrudService<T>` (create, get, update, delete). Soft-delete is auto-detected from the presence of a `deleted` field on the proto.

**Immutable entities** (monetization catalog) extend `ProtoBufImmutableService<T>`, which implements `ImmutableService<T>` (create, get only). No update, no delete, no soft-delete filtering.

```java
// Mutable — RBAC pattern
public interface GroupService extends CrudService<Rbac.Group> {
    CompletableFuture<List<Rbac.Group>> list(RequestContext rc);
    CompletableFuture<List<Rbac.Group>> getUserGroups(RequestContext rc);
}

public class RelationalGroupService extends ProtoBufCrudService<Rbac.Group>
        implements GroupService {
    public RelationalGroupService(RelationalClient rc,
                                   DistributedCacheClient cache, AsyncTaskQueue queue) {
        super(rc, cache, queue, Rbac.Group.getDefaultInstance(), "deleted");
    }
    // deleted = false appended automatically by base class
    public CompletableFuture<List<Rbac.Group>> list(RequestContext rc) {
        return cachedList(rc, FilterCriteria.eq("enterpriseId", authEnterpriseId(rc)).orderBy("name"));
    }
}

// Immutable — monetization catalog pattern
public interface AppService extends ImmutableService<Monetization.App> {
    CompletableFuture<List<Monetization.App>> list(RequestContext rc);
}

public class RelationalAppService extends ProtoBufImmutableService<Monetization.App>
        implements AppService {
    @Inject
    public RelationalAppService(RelationalClient rc,
                                 DistributedCacheClient cache, AsyncTaskQueue queue) {
        super(rc, cache, queue, Monetization.App.getDefaultInstance());
    }
    public CompletableFuture<List<Monetization.App>> list(RequestContext rc) {
        return cachedList(rc, FilterCriteria.none().orderBy("name"));
    }
    @Override
    protected Monetization.App withParent(Monetization.App parent, Monetization.App entityBase) {
        return entityBase.toBuilder().setVersion(parent.getVersion() + 1).setParentAppId(parent.getId()).build();
    }
}
```

Key points:
- `"deleted"` in the RBAC constructor's `excludeFromWrite` means it is never set on INSERT or UPDATE.
- `deleted = false` is **automatically appended** to all `FilterCriteria`-based queries by `ProtoBufCrudService.withActive()` — callers never write it explicitly.
- Monetization services have **no** `withActive()` — immutable records are never soft-deleted.
- `cachedList` in both cases uses `ProtoBufListCache` with read-through and async write-back via `taskQueue`.

### Association services (`ProtoBufAssociationService`)

Association services extend `ProtoBufAssociationService<T>`, which implements `AssociationService<T>`
and provides concrete `select(rc, FilterCriteria)`. No `mapRow` implementation needed — row mapping
is automatic. Subclasses only add domain-named mutation methods:

```java
public interface GroupMemberService extends AssociationService<Rbac.GroupMember> {
    CompletableFuture<Rbac.GroupMember> add(RequestContext rc, String groupId, String userId);
    CompletableFuture<Void> remove(RequestContext rc, String groupId, String userId);
    CompletableFuture<Optional<Rbac.GroupMember>> get(RequestContext rc, String groupId, String userId);
    CompletableFuture<Boolean> isUserInGroup(RequestContext rc, String groupId);
}

public class RelationalGroupMemberService extends ProtoBufAssociationService<Rbac.GroupMember>
        implements GroupMemberService {
    public RelationalGroupMemberService(RelationalClient relationalClient) {
        super(relationalClient, "groupMember", Rbac.GroupMember.getDefaultInstance(), "groupId", "userId");
    }
    public CompletableFuture<Rbac.GroupMember> add(RequestContext rc, String groupId, String userId) {
        return insert(rc, Rbac.GroupMember.newBuilder()
            .setEnterpriseId(authEnterpriseId(rc))
            .setGroupId(groupId).setUserId(userId).build());
    }
    public CompletableFuture<Void> remove(RequestContext rc, String groupId, String userId) {
        return deleteByKey(rc, Rbac.GroupMember.newBuilder()
            .setGroupId(groupId).setUserId(userId).build());
    }
}
```

Constructor varargs are the composite key fields in order (`"groupId", "userId"`).

### Cross-cutting queries

| Method | Service | SQL pattern |
|---|---|---|
| `getUsersInGroup(rc, groupId)` | `UserService` | Subquery on `groupMember` |
| `getUserGroups(rc)` | `GroupService` | Subquery on `groupMember` |
| `getUserRoles(rc)` | `RoleService` | UNION of `userRole` + `groupRole`/`groupMember` |
| `hasPermission(rc, permission)` | `RolePermissionService` | EXISTS across all role sources |
| `getUserPermissions(rc)` | `RolePermissionService` | DISTINCT UNION across all role sources |
| `isUserInGroup(rc, groupId)` | `GroupMemberService` | EXISTS with enterprise scope |
| `hasRole(rc, roleId)` | `UserRoleService` | EXISTS across direct + group-inherited roles |

---

## Soft delete vs hard delete

- All RBAC entity tables have a `deleted BOOLEAN` column. `store.softDelete(ctx, id)` sets `deleted = true`. This includes `Account`.
- `ApiKey` is hard-deleted on revoke (`erase`). No soft-delete column on `ApiKey`.
- Association tables (`groupMember`, `userRole`, `groupRole`, `rolePermission`, `accountUser`) have no `deleted` column — rows are simply removed.
- **Monetization catalog tables have no `deleted` column** — immutable entities are never deleted; a new version is created instead.
- `ProtoBufCrudService` detects the `deleted` field at construction and automatically appends `AND deleted = false` to every `FilterCriteria`-based query.

---

## Authentication

### OAuth (human users)

Human users authenticate via an IdP (Google, Firebase, Keycloak, etc.). The `User` record stores `externalId` (the IdP subject) and `idpProvider`. No passwords are stored.

`POST /auth/login` with `{"idToken":"<google-or-firebase-id-token>"}` validates the token and returns a short-lived access token.

Subsequent requests carry `Authorization: Bearer <jwt>` plus `X-Enterprise-Id: <id>`. The full auth chain:

1. `BaseServiceHandler` — no Bearer token → `401`
2. Token introspection via `OAuthClient.introspectToken` — inactive or blank subject → `401`
3. **`RbacUserContextResolver`** — verifies enterprise exists, looks up user by `externalId`, replaces IdP subject in `RequestContext.authContext.userId` with RBAC user ID

**Seed data note:** the bootstrap user record has `externalId = 'pending:shilpa@geekrox.com'` as a placeholder. Update it to the actual Google `sub` claim before testing authenticated endpoints.

### API keys (headless / programmatic access)

```
Authorization: Bearer <raw-api-key>
```

`BaseServiceHandler.handleHttp` detects a non-JWT bearer value and routes to `ApiKeyResolver`. The resolver SHA-256 hashes the raw key, looks it up by `keyHash`, validates expiry, and returns a `TokenIntrospection` carrying `subject = userId` and `enterpriseId`.

**Bootstrap seed key (dev only):**
```
raw key : pithos-dev-key-00000000000000000000
prefix  : pithos-dev-k
owner   : svc-dev@pithos.info  (service account, dev role)
```

---

## authz-app — handler and gRPC layer

### Handler pattern

Each entity has a `XxxHandlers` class containing one static `final` inner class per RPC operation, each extending `BaseServiceHandler<Req, Resp>` with dependencies via `@Inject`.

```java
public final class GroupHandlers {
    public static final class Create extends BaseServiceHandler<CreateGroupRequest, Group> {
        private final GroupService service;
        @Inject
        public Create(OAuthClient oAuthClient, GroupService service) {
            super(oAuthClient);
            this.service = service;
        }
        @Override
        public Uni<Group> handle(CreateGroupRequest req, RequestContext rc) {
            return Uni.createFrom().completionStage(() -> service.create(rc, req.getName()))
                .map(created -> ProtoBufMapper.map(created, Group.newBuilder()));
        }
    }
}
```

`BaseServiceHandler.handle(Req, RequestContext)` is called by both the REST and gRPC layers.

**RBAC handler files:** `EnterpriseHandlers`, `UserHandlers`, `GroupHandlers`, `RoleHandlers`, `ApiKeyHandlers`, `GroupMemberHandlers`, `UserRoleHandlers`, `GroupRoleHandlers`, `RolePermissionHandlers`.

**Monetization handler files:** `AppHandlers`, `FeatureHandlers`, `JourneyHandlers`, `WorkflowHandlers`, `WorkflowFeatureHandlers`.

### gRPC service layer

Each entity also has a `XxxGrpcService` that extends the generated `XxxServiceGrpc.XxxServiceImplBase` and delegates every RPC to the matching handler via `AuthzGrpcSupport.respond()`.

### REST resource layer

`XxxResource` classes mount routes on the Vert.x `Router` and delegate to the same handler classes via `BaseServiceHandler.route()`.

---

## Module and lifecycle

### Three-phase startup

```
Phase 1 — ApplicationContextImpl constructor    init() on all modules — no I/O
Phase 2 — appContext.start(30, SECONDS).join()  start() on all modules — connections + migrations
Phase 3 — grpcServer.start(); httpServer.start() — accept traffic
```

### PostgresRbacModule / CloudSqlRbacModule

`init()` constructs clients and all RBAC service instances — no network calls. `start()` chains: open DB pool → run RBAC Liquibase migrations inside a transaction → open cache pool.

### PostgresMonetizationModule / CloudSqlMonetizationModule

`init()` constructs the relational client, cache client (`RedisCacheClient` or `MemoryStoreCacheClient`), and all monetization service instances — each service receives both the relational and cache client via constructor. `start()` chains: open DB pool → run monetization Liquibase migrations inside a transaction → open cache pool. `shutdown()` closes the cache pool then the DB pool.

A failed migration rolls back cleanly. Both monetization modules also bind `DistributedCacheClient` and the concrete cache type in `configure()` so downstream code can inject either.

---

## Caching

### Cache key convention

```
{enterpriseId}:proto:{fullTypeName}:{qualifier}
```

For monetization catalog entities the qualifier is `appId` (the catalog partition key) rather than `enterpriseId`.

### What is cached

| Service | What | Cache key | Strategy |
|---|---|---|---|
| `EnterpriseService.get` | Single `Enterprise` by id | entity id | Read-through + async write-back |
| `GroupService.get` | Single `Group` by id | entity id | Same |
| `GroupService.list` | All groups for an enterprise | global list | `ProtoBufListCache` read-through + async write-back |
| `RoleService.get` | Single `Role` by id | entity id | Same as group |
| `RoleService.list` | All roles for an enterprise | global list | `ProtoBufListCache` read-through + async write-back |
| `AppService.list` | All apps (system catalog) | global list | `ProtoBufListCache` read-through + async write-back |
| `FeatureService.listByApp` | Features for an appId | `appId` | Keyed list; invalidated on new feature |
| `JourneyService.listByApp` | Journeys for an appId | `appId` | Keyed list; invalidated on new journey |
| `WorkflowService.listByJourney` | Workflows for a journeyId | `journeyId` | Keyed list; invalidated on new workflow |
| `WorkflowFeatureService.listByWorkflow` | Steps for a workflowId | `workflowId` | Keyed list; invalidated on add/remove |

Monetization catalog entities are read frequently and change rarely — ideal list-cache candidates. `WorkflowHandlers` assembles `WorkflowDetail` (workflow + steps + features) by composing cache hits from `WorkflowService`, `WorkflowFeatureService`, and `FeatureService`. In steady state, `WorkflowDetail` assembly makes zero DB calls.

### Cache-Control bypass

Any request with `Cache-Control: no-cache` skips the list cache and reads directly from the database. This is detected via `rc.getCacheControl()` in `ProtoBufImmutableService.isNoCache()` and applied to all `cachedList` and `cachedList(key)` call sites, including `RelationalWorkflowFeatureService`.

### Cache invalidation

| Mutation | Invalidated key |
|---|---|
| New `Journey` created | `journeyService.invalidateListCache(rc, journey.getAppId())` |
| New `Workflow` created | `workflowService.invalidateListCache(rc, workflow.getJourneyId())` |
| `WorkflowFeature` added or removed | `workflowFeatureService.invalidate(rc, workflowId)` |

Invalidation is chained inside each service's overridden `save()` (for journeys and workflows) or after `insert`/`deleteByKey` (for workflow features), so it runs transparently regardless of which handler triggers the mutation.

### Backend wiring

- **Cloud SQL modules** — `CloudSqlMonetizationModule` constructs and starts a `MemoryStoreCacheClient` (GCP Memorystore)
- **Postgres modules** — `PostgresMonetizationModule` constructs and starts a `RedisCacheClient`

Both modules wire the concrete type and `DistributedCacheClient` into Guice so that `ProtoBufImmutableService` subclasses can receive the cache via constructor injection.

---

## DDL conventions

- All tables use `UUID PRIMARY KEY DEFAULT gen_random_uuid()` for entity tables.
- Association tables use `PRIMARY KEY (col1, col2)` — no surrogate key.
- Quoted identifiers (`"groupId"`, `"enterpriseId"`) preserve camelCase in Postgres.
- No `ON DELETE CASCADE`. Foreign keys default to `ON DELETE RESTRICT`.
- Changelog files are under `src/main/resources/db/changelog/<backend>/`.

### RBAC migrations

| File | Description |
|---|---|
| `001-rbac-schema.sql` | Core RBAC tables: `enterprise`, `user`, `group`, `groupMember`, `role`, `userRole`, `groupRole`, `rolePermission`, `apiKey` |
| `002-system-bootstrap.sql` | System enterprise, built-in roles, and seed permissions |
| `003-pithos-bootstrap.sql` | Pithos platform service accounts and API keys |
| `004-account-schema.sql` | `account` table (with `PERSONAL`/`TEAM` CHECK constraint) and `accountUser` join table |

### Monetization migrations

| File | Description |
|---|---|
| `V001__create_app.xml` | `app` table — immutable, `slug UNIQUE` |
| `V002__create_feature.xml` | `feature` table — immutable |
| `V003__create_journey.xml` | `journey` table — immutable, `description`/`goal`/`outcomeStatement` as TEXT |
| `V004__create_workflow.xml` | `workflow` table — immutable, `depthLevel VARCHAR(50)` |
| `V005__create_workflow_feature.xml` | `workflowFeature` association — composite PK `(workflowId, featureId)` |

---

## Bruno client API

API scripts live in `client-api/` and use the [Bruno](https://www.usebruno.com/) format. Copy `environments/local.example.bru` to `environments/local.bru` and fill in your token and IDs.

**RBAC endpoints:**

| Folder | Endpoints |
|---|---|
| `auth/` | Login, Google/Firebase token exchange |
| `enterprises/` | CRUD for SaaS tenant roots |
| `users/` | CRUD + users-in-group lookup |
| `groups/` | CRUD |
| `roles/` | CRUD |
| `api-keys/` | Create, get, list, revoke |
| `group-members/` | Add, remove, list, is-user-in-group |
| `user-roles/` | Grant, revoke, list |
| `group-roles/` | Assign, unassign, list |
| `role-permissions/` | Add, remove, list |
| `me/` | Current-user identity, roles, groups, permissions |
| `accounts/` | Create (PERSONAL + TEAM), get, update, delete, list, get-my-accounts |
| `account-users/` | Add member, remove member, list members, is-user-in-account |

**Monetization endpoints:**

| Folder | Endpoints |
|---|---|
| `apps/` | Create, get, list, create-version |
| `features/` | Create, get, list-by-app, create-version |
| `journeys/` | Create, get, list-by-app, create-version |
| `workflows/` | Create, get (returns `WorkflowDetail`), list-by-journey (returns `WorkflowDetailList`), create-version |
| `workflow-features/` | Add feature to workflow, remove, list-by-workflow |

Environment variables used across scripts:

| Variable | Description |
|---|---|
| `baseUrl` | `http://127.0.0.1:8080` by default |
| `token` | Bearer token (secret) |
| `userId` | RBAC user ID for per-user operations |
| `enterpriseId` | Enterprise ID; pre-seeded as `1` for local dev |
| `accountId` | Account ID for account and account-user operations |
| `appId` | Monetization app ID — catalog partition key |
| `groupId` | Group ID |
| `roleId` | Role ID |
| `permission` | Permission string, e.g. `portfolios:read` |
