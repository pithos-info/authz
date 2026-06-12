# Pithos RBAC Service

Role-based access control for Pithos multi-tenant enterprises. Manages users, groups, roles, permissions, and API keys across Cloud SQL and Postgres backends.

---

## Module structure

```
rbac-model/     RBAC.proto (data layer types) + RBACService.proto (public API types + gRPC service defs)
rbac-service/   Service interfaces + relational implementations (data layer only)
rbac-cloudsql/  Cloud SQL (PostgreSQL-compatible) Guice module — used in GCP
rbac-postgres/  Postgres Guice module — used locally
rbac-app/       Runnable assembly: REST + gRPC servers, Guice wiring, YAML config loading
```

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

---

## Data model

All entities are protobuf messages defined in `RBAC.proto`. The schema has two kinds of message:

**Entity messages** — have a surrogate `id` (UUID), `utcCreatedAt`, `utcModifiedAt`, `deleted` (soft-delete flag).

| Message | Table | Notes |
|---|---|---|
| `Enterprise` | `enterprise` | Tenant root |
| `User` | `user` | IdP-authenticated; no passwords |
| `Group` | `group` | Named collection of users within an enterprise |
| `Role` | `role` | Built-in or custom, scoped to an enterprise |
| `ApiKey` | `apiKey` | Hard-deleted on revoke; no soft delete |

**Association messages** — no surrogate `id`; composite primary key led by `enterpriseId`.

| Message | Table | Composite PK |
|---|---|---|
| `GroupMember` | `groupMember` | `(enterpriseId, groupId, userId)` |
| `UserRole` | `userRole` | `(enterpriseId, userId, roleId)` |
| `GroupRole` | `groupRole` | `(enterpriseId, groupId, roleId)` |
| `RolePermission` | `rolePermission` | `(enterpriseId, roleId, permission)` |

`enterpriseId` is the leading PK component on all association tables — this doubles as the partition key for future distributed storage backends, and allows enterprise-scoped range scans without a join to the parent entity.

### Naming conventions

- String fields named `"id"` or ending with `"Id"` are UUID columns. `ProtoBufStatement` and `ProtoBufRelationalClient` auto-convert these to `java.util.UUID` for JDBC.
- `permission` is plain `TEXT` — it does not end with `"Id"` and is passed as a `String`.
- `grantedById` on `UserRole` follows the `*Id` convention so it maps to UUID correctly.

### No `ON DELETE CASCADE`

Foreign keys use `ON DELETE RESTRICT`. Cascading deletes at the DB level are unpredictable at scale and incompatible with soft deletes. Offboarding is handled by explicit deletion scripts that run in dependency order.

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

- `EnterpriseService` → `Rbac.Enterprise`
- `UserService` → `Rbac.User`
- `GroupService` → `Rbac.Group`
- `RoleService` → `Rbac.Role`
- `ApiKeyService` → `Rbac.ApiKey`
- `GroupMemberService` → `Rbac.GroupMember`
- `UserRoleService` → `Rbac.UserRole`
- `GroupRoleService` → `Rbac.GroupRole`
- `RolePermissionService` → `Rbac.RolePermission`

Cross-cutting reads (e.g. "users in a group") live in the service whose type is returned: `UserService.getUsersInGroup` returns `List<Rbac.User>`, `GroupService.getUserGroups` returns `List<Rbac.Group>`, `UserService.findByExternalId` returns `Optional<Rbac.User>` (used by auth to resolve IdP subject → RBAC user).

### Entity services (`ProtoBufCrudService`)

Entity services extend `ProtoBufCrudService<T>`, which implements `CrudService<T>` and provides
concrete `create`, `get`, `update`, `delete`. Subclasses only add domain-specific methods:

```java
// Interface — extends CrudService<T>; only domain methods declared here
public interface GroupService extends CrudService<Rbac.Group> {
    CompletableFuture<List<Rbac.Group>> list(RequestContext rc);
    CompletableFuture<List<Rbac.Group>> getUserGroups(RequestContext rc);
}

// Implementation — constructor + domain methods only
public class RelationalGroupService extends ProtoBufCrudService<Rbac.Group>
        implements GroupService {

    public RelationalGroupService(RelationalClient rc,
                                   DistributedCacheClient cache, AsyncTaskQueue queue) {
        super(rc, cache, queue, Rbac.Group.getDefaultInstance(), "deleted");
    }

    @Override
    public CompletableFuture<List<Rbac.Group>> list(RequestContext rc) {
        // deleted = false is appended automatically by the base class
        return cachedList(rc, FilterCriteria.eq("enterpriseId", authEnterpriseId(rc)).orderBy("name"));
    }

    @Override
    public CompletableFuture<List<Rbac.Group>> getUserGroups(RequestContext rc) {
        // Cross-table subquery must use PreparedQuery
        String sql = "SELECT " + store.statement().columnList()
            + " FROM \"group\""
            + " WHERE id IN (SELECT \"groupId\" FROM \"groupMember\" WHERE \"userId\" = ?)"
            + " AND \"enterpriseId\" = ? AND deleted = false ORDER BY name";
        return query(rc, new PreparedQuery(sql, new Object[]{authUserId(rc), authEnterpriseId(rc)}));
    }
}
```

Key points:
- `"deleted"` in the constructor's `excludeFromWrite` means it is never set on INSERT or UPDATE.
- `deleted = false` is **automatically appended** to all `FilterCriteria`-based queries by `withActive()` in the base class — callers never write it explicitly.
- Cross-table queries (subqueries, UNIONs) use `query(rc, PreparedQuery)` and carry explicit `deleted = false` in the SQL.
- `create(rc, entity)` is the universal pattern — entity construction (including `id` and `enterpriseId`) happens at the handler layer; `ProtoBufStatement.insert()` auto-generates a UUID if `id` is unset.

### Association services (`ProtoBufAssociationService`)

Association services extend `ProtoBufAssociationService<T>`, which implements `AssociationService<T>`
and provides concrete `select(rc, FilterCriteria)`. No `mapRow` implementation needed — row mapping
is automatic. Subclasses only add domain-named mutation methods:

```java
// Interface — extends AssociationService<T>; select(rc, FilterCriteria) inherited
public interface GroupMemberService extends AssociationService<Rbac.GroupMember> {
    CompletableFuture<Rbac.GroupMember> add(RequestContext rc, String groupId, String userId);
    CompletableFuture<Void> remove(RequestContext rc, String groupId, String userId);
    CompletableFuture<Optional<Rbac.GroupMember>> get(RequestContext rc, String groupId, String userId);
    CompletableFuture<Boolean> isUserInGroup(RequestContext rc, String groupId);
}

// Implementation — constructor + domain methods only; select is inherited
public class RelationalGroupMemberService extends ProtoBufAssociationService<Rbac.GroupMember>
        implements GroupMemberService {

    public RelationalGroupMemberService(RelationalClient relationalClient) {
        super(relationalClient, "groupMember", Rbac.GroupMember.getDefaultInstance(), "groupId", "userId");
    }

    @Override
    public CompletableFuture<Rbac.GroupMember> add(RequestContext rc, String groupId, String userId) {
        return insert(rc, Rbac.GroupMember.newBuilder()
            .setEnterpriseId(authEnterpriseId(rc))
            .setGroupId(groupId).setUserId(userId).build());
    }

    @Override
    public CompletableFuture<Void> remove(RequestContext rc, String groupId, String userId) {
        return deleteByKey(rc, Rbac.GroupMember.newBuilder()
            .setGroupId(groupId).setUserId(userId).build());
    }

    @Override
    public CompletableFuture<Optional<Rbac.GroupMember>> get(RequestContext rc, String groupId, String userId) {
        return getByKey(rc, Rbac.GroupMember.newBuilder()
            .setGroupId(groupId).setUserId(userId).build());
    }
    // isUserInGroup uses relationalClient directly for a custom EXISTS query
}
```

Key points:
- Constructor varargs are the composite key fields in order (`"groupId", "userId"`).
- `insert(rc, entity)` / `deleteByKey(rc, key)` / `getByKey(rc, key)` delegate to `ProtoBufStatement` for the SQL and `store.findAll` for automatic row mapping — no `mapRow` override.
- `select(rc, FilterCriteria)` — callers pass e.g. `FilterCriteria.eq("groupId", id)`.

### `ProtoBufStatement` composite-key operations

Built internally by `ProtoBufAssociationService` from the constructor varargs:

```java
stmt.insert(entity)              // INSERT INTO table (...) VALUES (...) RETURNING *
stmt.deleteByCompositeId(key)    // DELETE FROM table WHERE k1 = ? AND k2 = ?
stmt.selectByCompositeId(key)    // SELECT cols FROM table WHERE k1 = ? AND k2 = ?
```

A key-only message (composite key fields set, all others zero/empty) is sufficient for delete and select.

### `FilterCriteria` usage

```java
// Association service select — callers pass criteria; no deleted filter (association tables have none)
groupMemberService.select(rc, FilterCriteria.eq("groupId", groupId))
userRoleService.select(rc, FilterCriteria.eq("userId", userId))
rolePermissionService.select(rc, FilterCriteria.eq("roleId", roleId))

// Entity service list — deleted = false is appended automatically by ProtoBufCrudService
query(rc, FilterCriteria.eq("enterpriseId", authEnterpriseId(rc)).orderBy("name"))
cachedList(rc, FilterCriteria.none().orderBy("utcCreatedAt"))   // none() = no user condition

// Entity service findBy — deleted = false auto-appended; no need to include it
query(rc, FilterCriteria.eq("enterpriseId", authEnterpriseId(rc))
                        .and(FilterCriteria.eq("externalId", sub)))
```

`FilterCriteria` supports `eq`, `neq`, `like`, `ilike`, `gt`, `gte`, `lt`, `lte`, `and`, `or`, `orderBy`, and `none()`. Field names are validated against the protobuf descriptor at call time. `*Id` fields are auto-converted to UUID; plain strings are passed as-is. See the [relational-api README](../../runtime/data/relational-api/README.md) for the full API.

### Cross-cutting queries

Methods that JOIN across multiple tables are placed in the service whose type they return:

| Method | Service | SQL pattern |
|---|---|---|
| `getUsersInGroup(rc, groupId)` | `UserService` | Subquery on `groupMember` |
| `getUserGroups(rc)` | `GroupService` | Subquery on `groupMember` |
| `getUserRoles(rc)` | `RoleService` | UNION of `userRole` + `groupRole`/`groupMember` |
| `hasPermission(rc, permission)` | `RolePermissionService` | EXISTS across all role sources |
| `getUserPermissions(rc)` | `RolePermissionService` | DISTINCT UNION across all role sources |
| `isUserInGroup(rc, groupId)` | `GroupMemberService` | EXISTS with enterprise scope |
| `hasRole(rc, roleId)` | `UserRoleService` | EXISTS across direct + group-inherited roles |

Subqueries are preferred over JOINs for list queries when the outer table has columns that would be ambiguous with the joined table (e.g. `utcCreatedAt` on both `group` and `groupMember`).

---

---

## Soft delete vs hard delete

- All entity tables have a `deleted BOOLEAN` column. `store.softDelete(ctx, id)` sets `deleted = true`.
- `ApiKey` is hard-deleted on revoke (`erase`). No soft-delete column on `ApiKey`.
- Association tables (`groupMember`, `userRole`, `groupRole`, `rolePermission`) have no `deleted` column — rows are simply removed.
- `ProtoBufCrudService` detects the `deleted` field at construction and automatically appends `AND deleted = false` to every `FilterCriteria`-based query. `PreparedQuery` paths (cross-table subqueries) carry `deleted = false` explicitly in their SQL.

---

## Authentication

### OAuth (human users)

Human users authenticate via an IdP (Google, Firebase, Keycloak, etc.). The `User` record stores `externalId` (the IdP subject — Google `sub` claim, Firebase `sub`, etc.) and `idpProvider`. No passwords are stored in RBAC.

`POST /auth/login` with `{"idToken":"<google-or-firebase-id-token>"}` validates the token through `GcpIdentityOAuthClient` and returns a short-lived access token. See [auth module docs](../../runtime/auth/README.md) for supported identity flows.

Subsequent requests carry the token as `Authorization: Bearer <jwt>` plus `X-Enterprise-Id: <id>`. The full auth chain for every non-login request:

1. `BaseServiceHandler` — no Bearer token → `401`
2. Token introspection via `OAuthClient.introspectToken` — inactive or blank subject → `401`
3. **`RbacUserContextResolver`** (installed in `RbacAppModule`):
   - Verifies the enterprise (from `X-Enterprise-Id`) exists
   - Looks up the user by `externalId = token.sub` within that enterprise (`UserService.findByExternalId`)
   - Not found → `401`
   - Found → replaces the IdP subject in `RequestContext.authContext.userId` with the RBAC user ID

All downstream role and permission checks use the RBAC user ID, not the IdP subject.

**Seed data note:** the bootstrap user record has `externalId = 'pending:shilpa@geekrox.com'` as a placeholder. Update it to the actual Google `sub` claim (visible in any decoded ID token at the `sub` field) before testing authenticated endpoints.

### API keys (headless / programmatic access)

Service accounts that cannot perform an OAuth flow authenticate with a long-lived API key.

**Key structure:**

- `keyHash` — SHA-256 of the raw key, stored in the DB. The raw key is never persisted.
- `keyPrefix` — first 12 characters, shown in the UI for identification.
- `permissions` — explicit subset of the owning user's effective permissions.
- `expiresAt` — epoch millis; `0` means no expiry.
- `lastUsedAt` — updated asynchronously on every successful use.

**Auth flow:**

```
Authorization: Bearer <raw-api-key>
```

`BaseServiceHandler.handleHttp` detects that the bearer value is not a JWT (fewer than 2 dots) and routes to `ApiKeyResolver.resolve(bootstrap, rawKey)` instead of `OAuthClient.introspectToken`. The resolver:

1. SHA-256 hashes the raw key.
2. Queries `apiKey` by `keyHash` (unique index — single row lookup).
3. Validates the key is not expired.
4. Returns a `TokenIntrospection` carrying `subject = userId` and `enterpriseId` from the key record — clients do not need to send `X-Enterprise-Id`.
5. Fires a background `UPDATE "apiKey" SET "lastUsedAt" = now()` without blocking the response.

`RbacApiKeyResolver` (in `rbac-app`) implements `ApiKeyResolver` (in `service-container-core`) and is installed at startup via an eager Guice singleton — no handler changes required.

**Bootstrap seed key (dev only):**

```
raw key : pithos-dev-key-00000000000000000000
prefix  : pithos-dev-k
owner   : svc-dev@pithos.info  (service account, dev role)
```

---

## rbac-app — handler and gRPC layer

### Handler pattern

Each entity has a `XxxHandlers` class (in `info.pithos.rbac.app.handler`) containing one static `final` inner class per RPC operation. Each inner class extends `BaseServiceHandler<Req, Resp>` and receives its dependencies via `@Inject`.

```java
public final class GroupHandlers {
    private GroupHandlers() {}

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
    // ...
}
```

`BaseServiceHandler.handle(Req, RequestContext)` is called by both the REST and gRPC layers. The HTTP path goes through `handleHttp()` which performs OAuth token introspection first; the gRPC path calls `handle()` directly with a context built from gRPC metadata by `RbacGrpcSupport`.

Handlers that return enriched response types (e.g. `Enterprise` with embedded groups and roles, `User` with embedded groups and roles) use `RbacEnricher` to build the nested collections in parallel via `CompletableFuture.allOf`.

**9 handler files:** `EnterpriseHandlers`, `UserHandlers`, `GroupHandlers`, `RoleHandlers`, `ApiKeyHandlers`, `GroupMemberHandlers`, `UserRoleHandlers`, `GroupRoleHandlers`, `RolePermissionHandlers`.

### gRPC service layer

Each entity also has a `XxxGrpcService` (in `info.pithos.rbac.app.grpc`) that extends the generated `XxxServiceGrpc.XxxServiceImplBase` and delegates every RPC to the matching handler.

```java
public final class GroupGrpcService extends GroupServiceGrpc.GroupServiceImplBase {

    private final GroupHandlers.Create create;
    // ...

    @Override
    public void create(CreateGroupRequest request, StreamObserver<Group> responseObserver) {
        RbacGrpcSupport.respond(create.handle(request, RbacGrpcSupport.context()), responseObserver);
    }
}
```

`RbacGrpcSupport.context()` reads the gRPC `Metadata` stashed by `RbacGrpcInterceptor` and builds a `RequestContext` from it (enterprise id, user id, request id, etc.). `RbacGrpcSupport.respond()` subscribes to the `Uni`, calls `onNext` + `onCompleted` on success, and maps `ServiceException` error codes to gRPC `Status` values on failure.

**9 gRPC service files + `RbacGrpcSupport`:** `EnterpriseGrpcService`, `UserGrpcService`, `GroupGrpcService`, `RoleGrpcService`, `ApiKeyGrpcService`, `GroupMemberGrpcService`, `UserRoleGrpcService`, `GroupRoleGrpcService`, `RolePermissionGrpcService`.

> **Note — boilerplate:** the current structure has ~10 lines of wiring per RPC method spread across the handler inner-class and the gRPC service. With 9 entities × ~4 RPCs each that is ~360 lines that add no logic. This is the main area to review before finalising.

### REST resource layer

`XxxResource` classes (in `info.pithos.rbac.app.rest.resource`) mount routes on the Vert.x `Router` and delegate to the same handler classes:

```java
r.get("/enterprises").handler(ctx ->
    BaseServiceHandler.route(ctx, 200, list, Empty.getDefaultInstance()));

r.post("/enterprises").handler(ctx -> {
    CreateEnterpriseRequest req = BaseServiceHandler.parseBody(ctx, CreateEnterpriseRequest.newBuilder());
    if (req == null) return;
    BaseServiceHandler.route(ctx, 201, create, req);
});
```

`BaseServiceHandler.route()` calls `handleHttp()` (OAuth introspection + context build), subscribes to the `Uni`, and writes the proto as JSON on success or an `{"error":"..."}` JSON body with the appropriate HTTP status on failure.

---

## Module and lifecycle

The full lifecycle contract is documented in [`runtime/runtime-core`](../../runtime/runtime-core/README.md). This section covers the RBAC-specific modules.

### Three-phase startup

```
Phase 1 — ApplicationContextImpl constructor   init() on all modules — no I/O
Phase 2 — appContext.start(30, SECONDS).join() start() on all modules — connections + migrations
Phase 3 — grpcServer.start(); httpServer.start() — accept traffic
```

### PostgresRbacModule / CloudSqlRbacModule

`init()` constructs clients and all service instances — no network calls. `start()` chains: open DB pool → run Liquibase inside a transaction → open cache pool. `shutdown()` closes in reverse order.

```java
// init() — pure construction
this.relationalClient = new PostgresClient(this.getApplicationContext());
this.cacheClient      = new RedisCacheClient(this.getApplicationContext());
// userRoleService constructed first — EnterpriseService depends on it for system-admin checks
this.userRoleService   = new RelationalUserRoleService(relationalClient);
this.enterpriseService = new RelationalEnterpriseService(relationalClient, cacheClient, taskQueue, userRoleService);
// ... all other services

// start() — ordered: DB pool → Liquibase → cache pool
return relationalClient.start(timeout, unit)
    .thenCompose(ok -> relationalClient.transaction(conn -> runLiquibase(conn)))
    .thenCompose(v  -> cacheClient.start(timeout, unit));

// shutdown() — reverse order
return cacheClient.shutdown(timeout, unit)
    .thenCompose(ok -> relationalClient.shutdown(timeout, unit));
```

Liquibase runs inside a transaction — a failed migration rolls back cleanly.

### Other infrastructure modules

`KeycloakOAuthModule`, `HashiCorpVaultModule`, and `MinioBlobStorageModule` each own a single client. Their `start()` and `shutdown()` are one-liners that delegate directly to the client.

`RbacAppModule` has no lifecycle clients; it inherits the default no-op `start()` and `shutdown()` from `ServiceModule`.

---

## rbac-app — running the service

### Assembly

```
mvn package -pl rbac-app -am
java -Drbac.config=/path/to/config.yaml -jar rbac-app/target/rbac-app.jar
```

The assembly bundles all dependencies into a single fat jar. The main class is `info.pithos.rbac.app.RbacApp`.

### Startup sequence

```
1. RbacConfigLoader.load(path)         parse YAML → ConfigMap + httpPort + grpcPort
2. ApplicationContextImpl(creator)     init() all modules (constructs objects, no I/O)
3. appContext.start(30, SECONDS)        start() all modules in parallel (connections, migrations)
4. grpcServer.start()                  gRPC server binds port
5. httpServer.start()                  Vert.x HTTP server binds port
```

Shutdown reverses steps 5–3, then tears down executor pools via `ApplicationContext.shutdown()`.

### YAML config

Keys are exact proto field names (`camelCase`) so the YAML → JSON → `JsonFormat` path works without a mapping layer. The `server` block is the only non-proto section; it is ignored by `JsonFormat.parser().ignoringUnknownFields()`.

```yaml
server:
  httpPort: 8080
  grpcPort: 9090

bootstrapConfigs:
  serviceName: rbac-service
  multiplier: 2

postgresConfigs:   { host: localhost, port: 5432, ... }
redisConfigs:      { host: localhost, port: 6379, ... }
keycloakOAuthConfigs: { serverUrl: ..., realm: pithos, ... }
hashiCorpVaultConfigs: { address: http://localhost:8200, ... }
minioBlobStorageConfigs: { endpoint: http://localhost:9000, ... }
```

See `rbac-app/src/main/resources/rbac-config-example.yaml` for a complete template.

### Guice wiring

All bindings are registered in `RbacAppModule`. The module list in `RbacContextCreator` determines which infrastructure stack is used:

| Profile | Modules |
|---|---|
| Local | `PostgresRbacModule`, `KeycloakOAuthModule`, `HashiCorpVaultModule`, `MinioBlobStorageModule`, `RbacAppModule` |
| GCP | `CloudSqlRbacModule`, `GcpIdentityOAuthModule`, `GcpSecretManagerModule`, `GcsBlobStorageModule`, `RbacAppModule` |

Note: Redis (`RedisCacheClient`) is owned and bound by `PostgresRbacModule` — it is not a separate module entry. Adding a standalone `RedisCacheModule` alongside `PostgresRbacModule` would create a duplicate Guice binding and fail at injector creation.

`RbacAppModule` is profile-independent — it only binds handlers, gRPC services, and servers.

---

## Caching

### Cache key convention

All cache keys follow the format:

```
{enterpriseId}:proto:{fullTypeName}:{qualifier}
```

The `{enterpriseId}:` prefix is added automatically by `AbstractDistributedCacheClient.createKey`. The part you pass to the cache client is `proto:{fullTypeName}:{qualifier}`:

| What | Key passed | Full Redis key |
|---|---|---|
| Single entity by id | `proto:info.pithos.rbac.model.Group:{uuid}` | `{eid}:proto:info.pithos.rbac.model.Group:{uuid}` |
| Enterprise-wide list | `proto:info.pithos.rbac.model.Group:list` | `{eid}:proto:info.pithos.rbac.model.Group:list` |

The `proto:` segment identifies the serialization format, distinguishing protobuf-backed entries from other cache values (session tokens, rate-limit counters, etc.) in the same Redis instance.

### What is cached

| Service | What | Strategy |
|---|---|---|
| `EnterpriseService.get` | Single `Enterprise` by id | `ProtoBufRelationalClient` read-through + async write-back |
| `GroupService.get` | Single `Group` by id | Same |
| `GroupService.list` | All groups for an enterprise | `ProtoBufListCache` read-through + async write-back |
| `RoleService.get` | Single `Role` by id | Same as group |
| `RoleService.list` | All roles for an enterprise | `ProtoBufListCache` read-through + async write-back |

`getUserGroups` and `getUserRoles` are user-scoped queries and are not cached — their result sets vary per caller.

### Single-entity cache (`ProtoBufRelationalClient`)

Entity services wired with a `DistributedCacheClient` and `AsyncTaskQueue` get caching automatically:

- **`findById`** — cache-first; on a miss, queries the DB and enqueues a background write-back via a distributed lock (prevents stampede).
- **`insert`** — DB insert, then enqueues a background write-back.
- **`update`** — evicts the cache entry **before** the DB write (prevents stale read-through during the update window).
- **`softDelete` / `delete`** — evicts after a successful DB operation.

No code changes are needed in the service — just pass `cacheClient` and `taskQueue` to the factory:

```java
this.store = ProtoBufRelationalClient.of(relationalClient, cacheClient, taskQueue,
                                         Rbac.Group.getDefaultInstance(), "deleted");
```

### List cache (`ProtoBufListCache`)

`ProtoBufListCache<T>` stores a `List<T>` as a binary blob using `ProtoBufSerde` per element (delimited wire format), consistent with how `ProtoBufCache` serializes single entities.

```java
this.listCache = ProtoBufListCache.of(cacheClient, Rbac.Group.getDefaultInstance());

// Read-through
listCache.get(rc, listCache.listKey()).thenCompose(cached -> {
    if (cached != null) return CompletableFuture.completedFuture(cached);
    return queryDb().thenApply(list -> {
        taskQueue.enqueue(() -> listCache.set(rc, listCache.listKey(), list)); // async
        return list;
    });
});

// Synchronous eviction on any mutation
listCache.delete(rc, listCache.listKey())
```

`listKey()` returns `proto:{fullTypeName}:list` derived from the prototype — services never construct the key string manually.

### Cache invalidation on mutation

List cache is evicted synchronously (as part of the `CompletableFuture` chain) after every successful mutation so a subsequent list call always reflects the change:

```java
// create/update — evict list after DB write, then return the entity
store.insert(dc(rc), group)
    .thenCompose(created -> listCache.delete(rc, listCache.listKey())
        .thenApply(d -> created));

// delete — evict list after soft-delete
store.softDelete(dc(rc), id)
    .thenCompose(n -> listCache.delete(rc, listCache.listKey()))
    .thenAccept(d -> {});
```

Write-back on cache miss is async (via `taskQueue.enqueue`) so the DB query is not held up waiting for Redis.

### Backend wiring

- **Cloud SQL module** (`CloudSqlRbacModule`) — uses `MemoryStoreCacheClient` (GCP Memorystore).
- **Postgres module** (`PostgresRbacModule`) — uses `RedisCacheClient`.

Both open their cache connection in `start()` (after the DB pool is up and migrations have run), not in `init()`. The cache is guaranteed to be ready before the HTTP/gRPC servers accept traffic because `RbacApp` calls `appContext.start().join()` before binding ports.

---

## Auto-timestamps in `ProtoBufStatement`

`ProtoBufStatement` auto-populates timestamp fields at the **application layer** so that all timestamps originate from the app clock, not the database node. The indices are computed once at construction time — the per-call check is O(1).

| Operation | Field | Behaviour |
|---|---|---|
| `insert()` | `utcCreatedAt` | Auto-set to `System.currentTimeMillis()` if unset (0) |
| `update()` | `utcModifiedAt` | Auto-set to `System.currentTimeMillis()` if unset (0) |

Both fields are **optional by field existence** — if the proto message does not declare the field (e.g. `utcModifiedAt` is absent from `Group`, `Role`, and all association tables), the index resolves to `-1` and the check is skipped silently. No caller changes are needed for those types.

Callers that explicitly set either field to a non-zero value retain that value — the auto-set only fires when the field is at its proto3 default (`0`).

`utcCreatedAt` is already excluded from `updateFields` by the constructor (it is written once on INSERT and never overwritten). `utcModifiedAt` is excluded from `insertFields` only if passed to `excludeFromWrite` — otherwise it participates in both INSERT and UPDATE SET.

**Consequence for services:** do not call `setUtcCreatedAt(...)` or `setUtcModifiedAt(...)` manually in service create/update methods. The statement layer handles it uniformly.

---

## DDL conventions

- All tables use `UUID PRIMARY KEY DEFAULT gen_random_uuid()` for entity tables.
- Association tables use `PRIMARY KEY (col1, col2)` — no surrogate key.
- Quoted identifiers (`"groupId"`, `"enterpriseId"`) preserve camelCase in Postgres.
- No `ON DELETE CASCADE`. Foreign keys default to `ON DELETE RESTRICT`.
- Changelog files are under `src/main/resources/db/changelog/<backend>/`.
