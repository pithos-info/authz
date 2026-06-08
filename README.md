# Pithos RBAC Service

Role-based access control for Pithos multi-tenant enterprises. Manages users, groups, roles, permissions, and API keys across Cloud SQL and Postgres backends.

---

## Module structure

```
rbac-model/     RBAC.proto — all data types as protobuf messages
rbac-service/   Service interfaces + relational implementations
rbac-cloudsql/  Cloud SQL (PostgreSQL-compatible) Guice module
rbac-postgres/  Postgres Guice module
```

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

**Association messages** — no surrogate `id`; composite primary key of their FK fields.

| Message | Table | Composite PK |
|---|---|---|
| `GroupMember` | `groupMember` | `(groupId, userId)` |
| `UserRole` | `userRole` | `(userId, roleId)` |
| `GroupRole` | `groupRole` | `(groupId, roleId)` |
| `RolePermission` | `rolePermission` | `(roleId, permission)` |

### Naming conventions

- String fields named `"id"` or ending with `"Id"` are UUID columns. `ProtoBufStatement` and `ProtoBufRelationalClient` auto-convert these to `java.util.UUID` for JDBC.
- `permission` is plain `TEXT` — it does not end with `"Id"` and is passed as a `String`.
- `grantedById` on `UserRole` follows the `*Id` convention so it maps to UUID correctly.

### No `ON DELETE CASCADE`

Foreign keys use `ON DELETE RESTRICT`. Cascading deletes at the DB level are unpredictable at scale and incompatible with soft deletes. Offboarding is handled by explicit deletion scripts that run in dependency order.

---

## Service design

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

Cross-cutting reads (e.g. "users in a group") live in the service whose type is returned: `UserService.getUsersInGroup` returns `List<Rbac.User>`, `GroupService.getUserGroups` returns `List<Rbac.Group>`.

### Entity services (`ProtoBufRelationalClient`)

Entity services (single surrogate `id`) use `ProtoBufRelationalClient<M>` as their sole data-access field:

```java
public class RelationalGroupService extends AbstractRbacService implements GroupService {

    private final ProtoBufRelationalClient<Rbac.Group> store;

    public RelationalGroupService(RelationalClient relationalClient) {
        super(relationalClient);
        this.store = ProtoBufRelationalClient.of(relationalClient,
                                                 Rbac.Group.getDefaultInstance(), "deleted");
    }

    @Override
    public CompletableFuture<Optional<Rbac.Group>> get(RequestContext rc, String id) {
        return store.findById(dc(rc), id).thenApply(Optional::ofNullable);
    }

    @Override
    public CompletableFuture<List<Rbac.Group>> list(RequestContext rc) {
        String sql = "SELECT " + store.statement().columnList()
            + " FROM \"group\" WHERE \"enterpriseId\" = ? AND deleted = false ORDER BY name";
        return store.findAll(dc(rc), new PreparedQuery(sql, new Object[]{authEnterpriseId(rc)}));
    }
}
```

Key points:
- `store.insert/update/softDelete/delete` for CRUD — no `Row` mapper needed; `ProtoBufRelationalClient` maps rows via field-name introspection.
- `store.findAll(ctx, PreparedQuery)` for custom list/filter queries where `ORDER BY` is needed.
- `store.findAll(ctx, FilterCriteria)` for ad-hoc predicate queries (no `ORDER BY`).
- `store.statement().columnList()` to get the quoted column list for custom SQL.
- `"deleted"` is passed to the factory as an excluded-from-write field — it is never set on INSERT or UPDATE SET.

### Association services (`ProtoBufStatement` + `ProtoBufRelationalClient`)

Association tables have a composite PK and no `id` field. They use two fields:

```java
public class RelationalGroupMemberService extends AbstractRbacService implements GroupMemberService {

    // Composite-key STMT for insert / delete / point-select
    private static final ProtoBufStatement<Rbac.GroupMember> STMT =
        ProtoBufStatement.of("groupMember", Rbac.GroupMember.getDefaultInstance(),
                             new String[]{"groupId", "userId"});

    // ProtoBufRelationalClient for FilterCriteria-based select
    private final ProtoBufRelationalClient<Rbac.GroupMember> store;

    public RelationalGroupMemberService(RelationalClient relationalClient) {
        super(relationalClient);
        this.store = ProtoBufRelationalClient.of(relationalClient, "groupMember",
                                                 Rbac.GroupMember.getDefaultInstance(), "groupId");
    }

    @Override
    public CompletableFuture<Rbac.GroupMember> add(RequestContext rc, String groupId, String userId) {
        Rbac.GroupMember member = Rbac.GroupMember.newBuilder()
            .setGroupId(groupId).setUserId(userId)
            .setUtcCreatedAt(System.currentTimeMillis()).build();
        return relationalClient.query(dc(rc), STMT.insert(member))
            .thenApply(rows -> toGroupMember(rows.get(0)));  // manual mapper needed here
    }

    @Override
    public CompletableFuture<Void> remove(RequestContext rc, String groupId, String userId) {
        Rbac.GroupMember key = Rbac.GroupMember.newBuilder()
            .setGroupId(groupId).setUserId(userId).build();
        return relationalClient.execute(dc(rc), STMT.deleteByCompositeId(key)).thenAccept(n -> {});
    }

    @Override
    public CompletableFuture<List<Rbac.GroupMember>> select(RequestContext rc, FilterCriteria filter) {
        return store.findAll(dc(rc), filter);  // automatic row mapping
    }
}
```

Key points:
- `ProtoBufStatement.of(table, proto, new String[]{"col1", "col2"})` — composite-key factory. Enables `insert`, `deleteByCompositeId`, `selectByCompositeId`.
- `ProtoBufRelationalClient.of(client, table, proto, "firstKeyField")` — used only for `findAll(FilterCriteria)`. The `"firstKeyField"` satisfies the constructor but is never used for single-id lookups.
- Manual `toXxx(Row)` mapper is still needed for `get` (uses `STMT.selectByCompositeId`) and `add` (uses `STMT.insert` which returns rows).
- `select(RequestContext, FilterCriteria)` replaces all `listByX` methods — callers pass `FilterCriteria.eq("groupId", id)` etc.

### `ProtoBufStatement` composite-key operations

`ProtoBufStatement` built with `String[] compositeIdFields` gains three new methods:

```java
STMT.deleteByCompositeId(message)  // DELETE FROM table WHERE k1 = ? AND k2 = ?
STMT.selectByCompositeId(message)  // SELECT cols FROM table WHERE k1 = ? AND k2 = ?
```

Values are extracted from the message fields using the same UUID/String/Timestamp type mapping as `insert`. A key-only message (fields set, `utcCreatedAt` zero) is sufficient for delete and select.

### `FilterCriteria` usage

```java
// List all members of a group
groupMemberService.select(rc, FilterCriteria.eq("groupId", groupId))

// List all roles assigned directly to a user
userRoleService.select(rc, FilterCriteria.eq("userId", userId))

// List all permissions for a role
rolePermissionService.select(rc, FilterCriteria.eq("roleId", roleId))
```

`FilterCriteria` supports `eq`, `neq`, `like`, `ilike`, `gt`, `gte`, `lt`, `lte` and logical `and`/`or` combinators. Field names are validated against the protobuf descriptor at call time. `*Id` fields are auto-converted to UUID; plain strings are passed as-is.

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

## `AbstractRbacService`

All service impls extend `AbstractRbacService`, which provides three static helpers:

```java
protected static DataContext dc(RequestContext rc)
protected static UUID authEnterpriseId(RequestContext rc)
protected static UUID authUserId(RequestContext rc)
```

Entity services pass `RelationalClient` to `super()` (required by the constructor) but use `store` for all data access. Association services use `relationalClient` directly for composite-key operations and raw SQL.

---

## Soft delete vs hard delete

- All entity tables have a `deleted BOOLEAN` column. `store.softDelete(ctx, id)` sets `deleted = true`.
- `ApiKey` is hard-deleted on revoke (`store.delete`). No soft-delete column.
- Association tables (`groupMember`, `userRole`, `groupRole`, `rolePermission`) have no `deleted` column — rows are simply removed.
- All `list` and cross-cutting queries filter `deleted = false` explicitly.

---

## Module and lifecycle

`RbacServiceModule` (abstract) holds all service fields and registers Guice bindings. Concrete modules (`CloudSqlRbacModule`, `PostgresRbacModule`) override `init()` to:

1. Construct the `RelationalClient` (HikariCP pool).
2. Start the pool and run Liquibase migrations synchronously (`.join()`).
3. Construct all services.

```java
@Override
public boolean init() {
    if (this.initialized.compareAndSet(false, true)) {
        this.relationalClient = new PostgresClient(this.getApplicationContext());
        this.relationalClient.start(30, TimeUnit.SECONDS)
            .thenCompose(started -> this.relationalClient.transaction(conn -> {
                Database db = DatabaseFactory.getInstance()
                    .findCorrectDatabaseImplementation(new JdbcConnection(conn));
                new Liquibase(CHANGELOG, new ClassLoaderResourceAccessor(), db)
                    .update(new Contexts(), new LabelExpression());
            }))
            .join();
        // construct services ...
    }
    return this.initialized.get();
}
```

Migrations run inside a transaction so a failed migration leaves the schema in a consistent state.

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

Both start the cache client synchronously in `init()` (`.join()`) before constructing services, ensuring Redis is reachable before the first request is served.

---

## DDL conventions

- All tables use `UUID PRIMARY KEY DEFAULT gen_random_uuid()` for entity tables.
- Association tables use `PRIMARY KEY (col1, col2)` — no surrogate key.
- Quoted identifiers (`"groupId"`, `"enterpriseId"`) preserve camelCase in Postgres.
- No `ON DELETE CASCADE`. Foreign keys default to `ON DELETE RESTRICT`.
- Changelog files are under `src/main/resources/db/changelog/<backend>/`.
