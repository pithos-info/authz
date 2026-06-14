# RBAC gRPC + REST API Implementation Guide

Use this document when asked to implement a new API entity in the RBAC service.
Before writing any code, collect the inputs in the **Gather inputs** section, then
work through the steps in order. Every step must be completed for the entity to be
fully wired.

---

## Gather inputs

Ask the developer for the following before generating anything:

1. **Entity name** — PascalCase singular (e.g. `Permission`, `Tenant`)
2. **Entity shape** — one of:
   - `crud` — owns its own rows; has create / get / update / delete / list
   - `association` — join between two existing entities (e.g. GroupMember, UserRole)
3. **Proto fields** — the fields on the entity message and on each request message
4. **Extra operations** — any operations beyond the standard set for the shape
   (e.g. `GetUserRoles`, `IsUserInGroup`, `HasPermission`)
5. **REST routes** — HTTP method + path for every operation
6. **Service dependencies** — which `*Service` interfaces the handlers need injected

---

## Project layout reference

```
rbac-model/src/main/proto/
  RBACService.proto                        ← proto messages + service definitions

rbac-app/src/main/java/info/pithos/rbac/app/
  handler/   *Handlers.java                ← business logic, one inner class per op
  grpc/      *GrpcService.java             ← gRPC stub dispatch
  rest/resource/  *Resource.java           ← REST route mounting
  RbacAppModule.java                       ← Guice bindings
  server/RbacGrpcServer.java               ← gRPC server wiring
  rest/RbacRestRouter.java                 ← REST router wiring
```

Key framework classes (all in `info.pithos.service.container.core`):
- `BaseServiceHandler<Req, Resp>` — base for every handler inner class
- `BaseServiceHandler.route(ctx, status, handler, req)` — REST dispatch with body
- `BaseServiceHandler.routeNoContent(ctx, handler, req)` — REST dispatch, 204
- `BaseServiceHandler.parseBody(ctx, builder)` — parse JSON body into proto; returns null on error
- `GrpcSupport.context()` — builds `RequestContext` from gRPC metadata
- `GrpcSupport.respond(uni, observer)` — subscribes Uni to StreamObserver

---

## Step 1 — Proto: messages + service

File: `rbac-model/src/main/proto/RBACService.proto`

Add at the bottom of the file, under a `// ─── EntityName ─` section header.

### CRUD shape

```proto
// ─── {Entity} ────────────────────────────────────────────────────────────────

message Create{Entity}Request {
  // one field per input
}

message Update{Entity}Request {
  string id = 1;
  // updatable fields
}

message {Entity} {
  string id            = 1;
  string enterpriseId  = 2;
  // entity fields
  int64  utcCreatedAt  = N;
  int64  utcModifiedAt = N;
}

message {Entity}List {
  repeated {Entity} {entities} = 1;
}

service {Entity}Service {
  rpc Create(Create{Entity}Request) returns ({Entity});
  rpc Get(GetByIdRequest)           returns ({Entity});
  rpc Update(Update{Entity}Request) returns ({Entity});
  rpc Delete(DeleteByIdRequest)     returns (Empty);
  rpc List(Empty)                   returns ({Entity}List);
  // extra operations if any
}
```

### Association shape

```proto
// ─── {EntityA}{EntityB} ──────────────────────────────────────────────────────

message Add{EntityA}{EntityB}Request {
  string {entityAId} = 1;
  string {entityBId} = 2;
}

message Remove{EntityA}{EntityB}Request {
  string {entityAId} = 1;
  string {entityBId} = 2;
}

message {EntityA}{EntityB} {
  string {entityAId} = 1;
  string {entityBId} = 2;
  int64  utcCreatedAt = 3;
}

message {EntityA}{EntityB}List {
  repeated {EntityA}{EntityB} {items} = 1;
}

service {EntityA}{EntityB}Service {
  rpc Add(Add{EntityA}{EntityB}Request)        returns ({EntityA}{EntityB});
  rpc Remove(Remove{EntityA}{EntityB}Request)  returns (Empty);
  rpc ListBy{EntityA}(GetByIdRequest)          returns ({EntityA}{EntityB}List);
  // extra operations (HasX, IsXInY, etc.)
}
```

After editing the proto, run the project build so protoc regenerates the Java sources
before writing any handler or gRPC service code.

---

## Step 2 — Handlers

File: `rbac-app/src/main/java/info/pithos/rbac/app/handler/{Entity}Handlers.java`

One outer `final` class with a private constructor. Each operation is a `public static final`
inner class extending `BaseServiceHandler<Req, Resp>`.

```java
package info.pithos.rbac.app.handler;

import com.google.inject.Inject;
import info.pithos.auth.OAuthClient;
import info.pithos.rbac.{Entity}Service;
// ... service + proto imports
import info.pithos.runtime.model.protocol.Context.RequestContext;
import info.pithos.service.container.core.BaseServiceHandler;
import io.smallrye.mutiny.Uni;

public final class {Entity}Handlers {

    private {Entity}Handlers() {}

    public static final class Create extends BaseServiceHandler<Create{Entity}Request, {Entity}> {
        private final {Entity}Service service;

        @Inject
        public Create(OAuthClient oAuthClient, {Entity}Service service) {
            super(oAuthClient);
            this.service = service;
        }

        @Override
        public Uni<{Entity}> handle(Create{Entity}Request req, RequestContext rc) {
            Rbac.{Entity} data = Rbac.{Entity}.newBuilder()
                // map req fields → data fields
                .build();
            return Uni.createFrom().completionStage(() -> service.create(rc, data))
                .map(created -> ProtoBufMapper.map(created, {Entity}.newBuilder()));
        }
    }

    public static final class Get extends BaseServiceHandler<GetByIdRequest, {Entity}> {
        private final {Entity}Service service;

        @Inject
        public Get(OAuthClient oAuthClient, {Entity}Service service) {
            super(oAuthClient);
            this.service = service;
        }

        @Override
        public Uni<{Entity}> handle(GetByIdRequest req, RequestContext rc) {
            return Uni.createFrom().completionStage(() -> service.get(rc, req.getId()))
                .map(opt -> opt.map(d -> ProtoBufMapper.<{Entity}>map(d, {Entity}.newBuilder()))
                    .orElseThrow(() -> new ServiceException(ErrorCode.NOT_FOUND,
                        "{Entity} not found: " + req.getId())));
        }
    }

    public static final class Update extends BaseServiceHandler<Update{Entity}Request, {Entity}> {
        private final {Entity}Service service;

        @Inject
        public Update(OAuthClient oAuthClient, {Entity}Service service) {
            super(oAuthClient);
            this.service = service;
        }

        @Override
        public Uni<{Entity}> handle(Update{Entity}Request req, RequestContext rc) {
            Rbac.{Entity} data = Rbac.{Entity}.newBuilder()
                .setId(req.getId())
                // map updatable fields
                .build();
            return Uni.createFrom().completionStage(() -> service.update(rc, data))
                .map(updated -> ProtoBufMapper.map(updated, {Entity}.newBuilder()));
        }
    }

    public static final class Delete extends BaseServiceHandler<DeleteByIdRequest, Empty> {
        private final {Entity}Service service;

        @Inject
        public Delete(OAuthClient oAuthClient, {Entity}Service service) {
            super(oAuthClient);
            this.service = service;
        }

        @Override
        public Uni<Empty> handle(DeleteByIdRequest req, RequestContext rc) {
            return Uni.createFrom().completionStage(() -> service.delete(rc, req.getId()))
                .map(v -> Empty.getDefaultInstance());
        }
    }

    public static final class List extends BaseServiceHandler<Empty, {Entity}List> {
        private final {Entity}Service service;

        @Inject
        public List(OAuthClient oAuthClient, {Entity}Service service) {
            super(oAuthClient);
            this.service = service;
        }

        @Override
        public Uni<{Entity}List> handle(Empty req, RequestContext rc) {
            return Uni.createFrom().completionStage(() -> service.list(rc))
                .map(items -> {Entity}List.newBuilder()
                    .addAll{Entities}(items.stream()
                        .map(d -> ProtoBufMapper.<{Entity}>map(d, {Entity}.newBuilder()))
                        .toList())
                    .build());
        }
    }

    // Add extra operation inner classes here following the same pattern
}
```

**Association shape** — use `Add`/`Remove`/`ListBy{EntityA}` as inner class names.
For boolean checks (`IsUserInGroup`, `HasRole`):

```java
public static final class Has{X} extends BaseServiceHandler<GetByIdRequest, Bool> {
    private final {Entity}Service service;

    @Inject
    public Has{X}(OAuthClient oAuthClient, {Entity}Service service) {
        super(oAuthClient);
        this.service = service;
    }

    @Override
    public Uni<Bool> handle(GetByIdRequest req, RequestContext rc) {
        return Uni.createFrom().completionStage(() -> service.has{X}(rc, req.getId()))
            .map(result -> Bool.newBuilder().setValue(result).build());
    }
}
```

---

## Step 3 — gRPC service

File: `rbac-app/src/main/java/info/pithos/rbac/app/grpc/{Entity}GrpcService.java`

```java
package info.pithos.rbac.app.grpc;

import com.google.inject.Inject;
import info.pithos.rbac.app.handler.{Entity}Handlers;
// ... proto imports
import info.pithos.service.container.core.grpc.GrpcSupport;
import io.grpc.stub.StreamObserver;

public final class {Entity}GrpcService extends {Entity}ServiceGrpc.{Entity}ServiceImplBase {

    private final {Entity}Handlers.Create create;
    private final {Entity}Handlers.Get    get;
    private final {Entity}Handlers.Update update;
    private final {Entity}Handlers.Delete delete;
    private final {Entity}Handlers.List   list;
    // extra handler fields

    @Inject
    public {Entity}GrpcService(
            {Entity}Handlers.Create create,
            {Entity}Handlers.Get    get,
            {Entity}Handlers.Update update,
            {Entity}Handlers.Delete delete,
            {Entity}Handlers.List   list) {
        this.create = create;
        this.get    = get;
        this.update = update;
        this.delete = delete;
        this.list   = list;
    }

    @Override
    public void create(Create{Entity}Request request, StreamObserver<{Entity}> responseObserver) {
        GrpcSupport.respond(create.handle(request, GrpcSupport.context()), responseObserver);
    }

    @Override
    public void get(GetByIdRequest request, StreamObserver<{Entity}> responseObserver) {
        GrpcSupport.respond(get.handle(request, GrpcSupport.context()), responseObserver);
    }

    @Override
    public void update(Update{Entity}Request request, StreamObserver<{Entity}> responseObserver) {
        GrpcSupport.respond(update.handle(request, GrpcSupport.context()), responseObserver);
    }

    @Override
    public void delete(DeleteByIdRequest request, StreamObserver<Empty> responseObserver) {
        GrpcSupport.respond(delete.handle(request, GrpcSupport.context()), responseObserver);
    }

    @Override
    public void list(Empty request, StreamObserver<{Entity}List> responseObserver) {
        GrpcSupport.respond(list.handle(request, GrpcSupport.context()), responseObserver);
    }

    // extra @Override methods follow the same one-liner pattern
}
```

Every method body is exactly: `GrpcSupport.respond(handler.handle(request, GrpcSupport.context()), responseObserver);`
No exceptions.

---

## Step 4 — REST resource

File: `rbac-app/src/main/java/info/pithos/rbac/app/rest/resource/{Entity}Resource.java`

```java
package info.pithos.rbac.app.rest.resource;

import com.google.inject.Inject;
import info.pithos.rbac.app.handler.{Entity}Handlers;
// ... proto imports
import info.pithos.service.container.core.BaseServiceHandler;
import io.vertx.ext.web.Router;

public final class {Entity}Resource {

    private final {Entity}Handlers.Create create;
    private final {Entity}Handlers.Get    get;
    private final {Entity}Handlers.Update update;
    private final {Entity}Handlers.Delete delete;
    private final {Entity}Handlers.List   list;

    @Inject
    public {Entity}Resource(
            {Entity}Handlers.Create create,
            {Entity}Handlers.Get    get,
            {Entity}Handlers.Update update,
            {Entity}Handlers.Delete delete,
            {Entity}Handlers.List   list) {
        this.create = create;
        this.get    = get;
        this.update = update;
        this.delete = delete;
        this.list   = list;
    }

    public void mount(Router r) {
        r.get("/{entities}").handler(ctx ->
            BaseServiceHandler.route(ctx, 200, list, Empty.getDefaultInstance()));

        r.post("/{entities}").handler(ctx -> {
            Create{Entity}Request req = BaseServiceHandler.parseBody(ctx, Create{Entity}Request.newBuilder());
            if (req == null) return;
            BaseServiceHandler.route(ctx, 201, create, req);
        });

        r.get("/{entities}/:id").handler(ctx ->
            BaseServiceHandler.route(ctx, 200, get,
                GetByIdRequest.newBuilder().setId(ctx.pathParam("id")).build()));

        r.put("/{entities}/:id").handler(ctx -> {
            Update{Entity}Request req = BaseServiceHandler.parseBody(ctx, Update{Entity}Request.newBuilder());
            if (req == null) return;
            BaseServiceHandler.route(ctx, 200, update,
                req.toBuilder().setId(ctx.pathParam("id")).build());
        });

        r.delete("/{entities}/:id").handler(ctx ->
            BaseServiceHandler.routeNoContent(ctx, delete,
                DeleteByIdRequest.newBuilder().setId(ctx.pathParam("id")).build()));

        // extra routes follow the same pattern
    }
}
```

Rules:
- `POST` (create) → status 201
- `GET`, `PUT` → status 200
- `DELETE` → `routeNoContent` (204, no body)
- Path params are injected via `ctx.pathParam("name")` and set on the request proto before dispatch
- `parseBody` returns null on failure and has already written the 400 response; always guard with `if (req == null) return`

---

## Step 5 — Guice bindings

File: `rbac-app/src/main/java/info/pithos/rbac/app/RbacAppModule.java`

Add to the `configure()` method in the appropriate section. Order: REST resource → handler inner classes → gRPC service.

```java
// ── {Entity} resource ─────────────────────────────────────────────────────────
bind({Entity}Resource.class).in(Singleton.class);

// ── {Entity} handlers ─────────────────────────────────────────────────────────
bind({Entity}Handlers.Create.class).in(Singleton.class);
bind({Entity}Handlers.Get.class).in(Singleton.class);
bind({Entity}Handlers.Update.class).in(Singleton.class);
bind({Entity}Handlers.Delete.class).in(Singleton.class);
bind({Entity}Handlers.List.class).in(Singleton.class);
// extra handler classes

// ── {Entity} gRPC service ─────────────────────────────────────────────────────
bind({Entity}GrpcService.class).in(Singleton.class);
```

Add all required imports at the top of the file.

---

## Step 6 — gRPC server

File: `rbac-app/src/main/java/info/pithos/rbac/app/server/RbacGrpcServer.java`

Three changes:

1. Add a field: `private final {Entity}GrpcService {entity}GrpcService;`
2. Add a constructor parameter and assignment (keep alphabetical order with existing services)
3. Add `.addService({entity}GrpcService)` in the `NettyServerBuilder` chain inside `start()`

---

## Step 7 — REST router

File: `rbac-app/src/main/java/info/pithos/rbac/app/rest/RbacRestRouter.java`

Three changes:

1. Add a field: `private final {Entity}Resource {entities};`
2. Add a constructor parameter and assignment
3. Add `{entities}.mount(router);` in the `mount()` method

---

## Checklist

Before declaring the entity done, verify all seven touch-points:

- [ ] Proto messages + service added to `RBACService.proto`
- [ ] `{Entity}Handlers.java` created with all operation inner classes
- [ ] `{Entity}GrpcService.java` created
- [ ] `{Entity}Resource.java` created
- [ ] `RbacAppModule.java` — bindings added for resource, all handler classes, gRPC service
- [ ] `RbacGrpcServer.java` — field, constructor param, and `.addService()` added
- [ ] `RbacRestRouter.java` — field, constructor param, and `.mount()` added
