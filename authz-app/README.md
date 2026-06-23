# authz-app

The runnable application module for the AuthZ service. Wires all infrastructure clients,
Guice bindings, gRPC services, and REST resources into a single process that serves both
protocols on separate ports.

---

## Startup sequence

`AuthZApp.main` drives a three-phase startup:

```
Phase 1 — construct
  AuthZConfigLoader.load()        reads authz-config.yaml → AuthZBootstrapConfig
  AuthZContextCreator             supplies modules + ConfigMap to ApplicationContext
  ApplicationContextImpl          builds the Guice injector from all modules (no I/O)

Phase 2 — connect
  ApplicationContext.start()      each ServiceModule opens its infrastructure clients
                                  (Postgres pool, Redis, Vault, MinIO, auth client)
                                  and runs Liquibase migrations in-process

Phase 3 — serve
  AuthZGrpcServer.start()         binds gRPC port, registers services + interceptor
  AuthZHttpServer.start()         binds HTTP port, mounts all REST routes via Vert.x
```

Shutdown is symmetric: HTTP → gRPC → infrastructure clients → executor pools.

---

## Module wiring (`AuthZContextCreator`)

`AuthZContextCreator` implements `ContextCreator` and returns the ordered list of
`ServiceModule`s that form the Guice injector:

| Module | Provides |
|---|---|
| `PostgresRbacModule` | RBAC relational client + all RBAC service impls; runs RBAC Liquibase migrations |
| `PostgresMonetizationModule` | Monetization relational client + all monetization service impls; runs monetization Liquibase migrations |
| `GcpIdentityOAuthModule` *or* `BypassAuthModule` | `OAuthClient` binding (see Auth modes) |
| `HashiCorpVaultModule` | `VaultClient` |
| `MinioBlobStorageModule` | `BlobStorageClient` |
| `AuthZAppModule` | everything in the table below |

---

## Guice bindings (`AuthZAppModule`)

All singletons. Grouped by layer:

**Infrastructure / config**
- `AuthZServerConfig` — port values from YAML

**Servers and router**
- `AuthZHttpServer`, `AuthZGrpcServer`, `AuthZRestRouter`

**Auth**
- `ApiKeyResolver` → `RbacApiKeyResolver` — looks up API keys by SHA-256 hash
- `UserContextResolver` → `RbacUserContextResolver` — resolves enterprise + maps IdP sub → RBAC user ID
- `GrpcMetadataInterceptor` — stamps gRPC `Metadata` into the call `Context`
- `LoginHandler`, `AuthGrpcService` — provided via `@Provides` from the bound `OAuthClient`

**For each RBAC entity** (Enterprise, User, Group, Role, ApiKey, GroupMember, UserRole, GroupRole, RolePermission):
- `*Resource` — REST route mounting
- `*Handlers.{Op}` — one inner class per operation (Create, Get, Update, Delete, List, …)
- `*GrpcService` — gRPC stub dispatch

**For each monetization entity** (App, Feature, Journey, Workflow, WorkflowFeature):
- `*Resource` — REST route mounting
- `*Handlers.{Op}` — one inner class per operation
- `*GrpcService` — gRPC stub dispatch

---

## Package layout

```
info.pithos.authz.app/
  AuthZApp                        main entry point
  AuthZAppModule                  Guice bindings
  AuthZContextCreator             module list + ConfigMap
  auth/                           ApiKeyResolver + UserContextResolver impls
  config/                         AuthZBootstrapConfig, AuthZServerConfig, AuthZConfigLoader
  server/                         AuthZGrpcServer, AuthZHttpServer
  handler/
    rbac/                         RBAC handler inner-class files
    monetization/                 Monetization handler inner-class files
  grpc/
    rbac/                         RBAC gRPC service impls
    monetization/                 Monetization gRPC service impls
  rest/
    AuthZRestRouter
    resource/
      rbac/                       RBAC REST resource classes
      monetization/               Monetization REST resource classes
```

---

## Request flow — REST

```
HTTP request
  → Vert.x Router (BodyHandler parses body)
    → *Resource.mount() route handler
      → BaseServiceHandler.parseBody()     deserialise JSON → proto request
      → BaseServiceHandler.route()
          → handleHttp()
              → OAuthClient.introspectToken()   validate Bearer token
              → UserContextResolver.resolve()   enterprise check + userId mapping
              → handler.handle(req, rc)         business logic
          → respond 200/201/204 JSON  or  routingError → 4xx/5xx JSON
```

## Request flow — gRPC

```
gRPC call
  → GrpcMetadataInterceptor          stamps Metadata into gRPC Context
    → *GrpcService.{method}()
        → GrpcSupport.context()      builds RequestContext from Metadata
        → GrpcSupport.respond()
            → handler.handle(req, rc)   business logic
            → StreamObserver.onNext/onCompleted  or  onError(StatusException)
```

In both flows, `handler.handle()` is the same method — the protocol layer is
transparent to business logic.

---

## Auth modes

Controlled by `server.bypassAuth` in the YAML config (default `false`).

| Mode | `OAuthClient` binding | `UserContextResolver` |
|---|---|---|
| **normal** | `GcpIdentityOAuthClient` — validates Firebase/GCP JWTs | `RbacUserContextResolver` — validates enterprise + user in DB |
| **bypass** | `BypassOAuthClient` — accepts any Bearer token; promotes `X-User-Id` header as subject | `RbacUserContextResolver` — unchanged; enterprise + user must still exist in DB |

Bypass mode skips IdP token validation only. All authZ rules (enterprise membership,
role assignments, admin guards) run identically to production.

**Bypass usage:**
```
Authorization: Bearer anything
X-Enterprise-Id: <enterprise-id>
X-User-Id: <external-id-of-existing-user>
```

---

## Config (`authz-config-example.yaml`)

```
server:
  httpPort: 8080
  grpcPort: 9090
  bypassAuth: false
```

All other top-level keys (`postgresConfigs`, `gcpIdentityOAuthConfigs`, etc.) map
directly to `ConfigMap` proto field names and are parsed by the respective
`ServiceModule` during `init()`.

Run with:
```
java -Dauthz.config=/path/to/authz-config.yaml -jar target/authz-app.jar
```

---

## Adding a new entity

Follow `Grpc-Rest-Api.md` in the `service-container` runtime project. The checklist
covers all seven touch-points: proto, handlers, gRPC service, REST resource, Guice
bindings, gRPC server, and REST router.
