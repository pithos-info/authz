# rbac-app

The runnable application module for the RBAC service. Wires all infrastructure clients,
Guice bindings, gRPC services, and REST resources into a single process that serves both
protocols on separate ports.

---

## Startup sequence

`RbacApp.main` drives a three-phase startup:

```
Phase 1 — construct
  RbacConfigLoader.load()         reads rbac-config.yaml → RbacBootstrapConfig
  RbacContextCreator              supplies modules + ConfigMap to ApplicationContext
  ApplicationContextImpl          builds the Guice injector from all modules (no I/O)

Phase 2 — connect
  ApplicationContext.start()      each ServiceModule opens its infrastructure clients
                                  (Postgres pool, Redis, Vault, MinIO, auth client)

Phase 3 — serve
  RbacGrpcServer.start()          binds gRPC port, registers services + interceptor
  RbacHttpServer.start()          binds HTTP port, mounts all REST routes via Vert.x
```

Shutdown is symmetric: HTTP → gRPC → infrastructure clients → executor pools.

---

## Module wiring (`RbacContextCreator`)

`RbacContextCreator` implements `ContextCreator` and returns the ordered list of
`ServiceModule`s that form the Guice injector:

| Module | Provides |
|---|---|
| `PostgresRbacModule` | `DataSource`, all `*Repository` / `*RelationalService` impls |
| `GcpIdentityOAuthModule` *or* `BypassAuthModule` | `OAuthClient` binding (see Auth modes) |
| `HashiCorpVaultModule` | `VaultClient` |
| `MinioBlobStorageModule` | `BlobStorageClient` |
| `RbacAppModule` | everything in the table below |

---

## Guice bindings (`RbacAppModule`)

All singletons. Grouped by layer:

**Infrastructure / config**
- `RbacServerConfig` — port values from YAML

**Servers and router**
- `RbacHttpServer`, `RbacGrpcServer`, `RbacRestRouter`

**Auth**
- `ApiKeyResolver` → `RbacApiKeyResolver` — looks up API keys by SHA-256 hash
- `UserContextResolver` → `RbacUserContextResolver` — resolves enterprise + maps IdP sub → RBAC user ID
- `GrpcMetadataInterceptor` — stamps gRPC `Metadata` into the call `Context`
- `LoginHandler`, `AuthGrpcService` — provided via `@Provides` from the bound `OAuthClient`

**For each entity** (Enterprise, User, Group, Role, ApiKey, GroupMember, UserRole, GroupRole, RolePermission):
- `*Resource` — REST route mounting
- `*Handlers.{Op}` — one inner class per operation (Create, Get, Update, Delete, List, …)
- `*GrpcService` — gRPC stub dispatch

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

## Config (`rbac-config-example.yaml`)

```
server:
  httpPort: 8080
  grpcPort: 9090
  bypassAuth: false
```

All other top-level keys (`postgresConfigs`, `gcpIdentityOAuthConfigs`, etc.) map
directly to `ConfigMap` proto field names and are parsed by the respective
`ServiceModule` during `init()`.

---

## Adding a new entity

Follow `Grpc-Rest-Api.md` at the project root. The checklist covers all seven
touch-points: proto, handlers, gRPC service, REST resource, Guice bindings, gRPC
server, and REST router.
