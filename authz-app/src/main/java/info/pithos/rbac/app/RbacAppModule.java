/*
 * Copyright 2026 Pithos
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package info.pithos.rbac.app;

import com.google.inject.Provides;
import com.google.inject.Singleton;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import info.pithos.auth.OAuthClient;
import info.pithos.rbac.app.config.RbacServerConfig;
import info.pithos.rbac.app.grpc.ApiKeyGrpcService;
import info.pithos.rbac.app.grpc.EnterpriseGrpcService;
import info.pithos.rbac.app.grpc.GroupGrpcService;
import info.pithos.rbac.app.grpc.GroupMemberGrpcService;
import info.pithos.rbac.app.grpc.GroupRoleGrpcService;
import info.pithos.service.container.core.grpc.GrpcMetadataInterceptor;
import info.pithos.rbac.app.grpc.RoleGrpcService;
import info.pithos.rbac.app.grpc.RolePermissionGrpcService;
import info.pithos.rbac.app.grpc.UserGrpcService;
import info.pithos.rbac.app.grpc.UserRoleGrpcService;
import info.pithos.rbac.app.handler.ApiKeyHandlers;
import info.pithos.rbac.app.handler.EnterpriseHandlers;
import info.pithos.rbac.app.handler.GroupHandlers;
import info.pithos.rbac.app.handler.GroupMemberHandlers;
import info.pithos.rbac.app.handler.GroupRoleHandlers;
import info.pithos.rbac.app.handler.RoleHandlers;
import info.pithos.rbac.app.handler.RolePermissionHandlers;
import info.pithos.rbac.app.handler.UserHandlers;
import info.pithos.rbac.app.handler.UserRoleHandlers;
import info.pithos.rbac.app.auth.RbacApiKeyResolver;
import info.pithos.rbac.app.auth.RbacUserContextResolver;
import info.pithos.rbac.app.rest.RbacRestRouter;
import info.pithos.rbac.app.rest.resource.ApiKeyResource;
import info.pithos.rbac.app.rest.resource.AuthResource;
import info.pithos.rbac.app.rest.resource.EnterpriseResource;
import info.pithos.rbac.app.rest.resource.GroupMemberResource;
import info.pithos.rbac.app.rest.resource.GroupResource;
import info.pithos.rbac.app.rest.resource.GroupRoleResource;
import info.pithos.rbac.app.rest.resource.MeResource;
import info.pithos.rbac.app.rest.resource.RolePermissionResource;
import info.pithos.rbac.app.rest.resource.RoleResource;
import info.pithos.rbac.app.rest.resource.UserResource;
import info.pithos.rbac.app.rest.resource.UserRoleResource;
import info.pithos.rbac.app.server.RbacGrpcServer;
import info.pithos.rbac.app.server.RbacHttpServer;
import info.pithos.runtime.core.context.ApplicationContext;
import info.pithos.runtime.core.context.ServiceModule;
import info.pithos.service.container.core.auth.ApiKeyResolver;
import info.pithos.service.container.core.auth.UserContextResolver;
import info.pithos.service.container.core.BaseServiceHandler;
import info.pithos.service.container.core.LoginHandler;
import info.pithos.service.container.core.grpc.AuthGrpcService;

final class RbacAppModule extends ServiceModule {

    private final int httpPort;
    private final int grpcPort;
    private final boolean bypassAuth;

    RbacAppModule(ApplicationContext context, int httpPort, int grpcPort, boolean bypassAuth) {
        super(context);
        this.httpPort = httpPort;
        this.grpcPort = grpcPort;
        this.bypassAuth = bypassAuth;
    }

    @Override
    public boolean init() {
        this.initialized.compareAndSet(false, true);
        return this.initialized.get();
    }

    @Override
    protected void configure() {
        super.configure();

        // Server config (ports)
        bind(RbacServerConfig.class).toInstance(new RbacServerConfig(httpPort, grpcPort, bypassAuth));

        // ── Servers & router ─────────────────────────────────────────────────
        bind(RbacHttpServer.class).in(Singleton.class);
        bind(RbacGrpcServer.class).in(Singleton.class);
        bind(RbacRestRouter.class).in(Singleton.class);

        // ── REST resource classes ─────────────────────────────────────────────
        bind(AuthResource.class).in(Singleton.class);
        bind(EnterpriseResource.class).in(Singleton.class);
        bind(UserResource.class).in(Singleton.class);
        bind(GroupResource.class).in(Singleton.class);
        bind(RoleResource.class).in(Singleton.class);
        bind(ApiKeyResource.class).in(Singleton.class);
        bind(GroupMemberResource.class).in(Singleton.class);
        bind(UserRoleResource.class).in(Singleton.class);
        bind(GroupRoleResource.class).in(Singleton.class);
        bind(RolePermissionResource.class).in(Singleton.class);
        bind(MeResource.class).in(Singleton.class);

        // ── API key auth ──────────────────────────────────────────────────────
        bind(ApiKeyResolver.class).to(RbacApiKeyResolver.class).in(Singleton.class);
        bind(ApiKeyResolverInstaller.class).asEagerSingleton();

        // ── User context resolution (enterprise + externalId → RBAC user ID) ─
        bind(UserContextResolver.class).to(RbacUserContextResolver.class).in(Singleton.class);
        bind(UserContextResolverInstaller.class).asEagerSingleton();

        // ── gRPC interceptor ─────────────────────────────────────────────────
        bind(GrpcMetadataInterceptor.class).in(Singleton.class);

        // ── gRPC services ────────────────────────────────────────────────────
        bind(EnterpriseGrpcService.class).in(Singleton.class);
        bind(UserGrpcService.class).in(Singleton.class);
        bind(GroupGrpcService.class).in(Singleton.class);
        bind(RoleGrpcService.class).in(Singleton.class);
        bind(ApiKeyGrpcService.class).in(Singleton.class);
        bind(GroupMemberGrpcService.class).in(Singleton.class);
        bind(UserRoleGrpcService.class).in(Singleton.class);
        bind(GroupRoleGrpcService.class).in(Singleton.class);
        bind(RolePermissionGrpcService.class).in(Singleton.class);

        // ── Enterprise handlers ───────────────────────────────────────────────
        bind(EnterpriseHandlers.Create.class).in(Singleton.class);
        bind(EnterpriseHandlers.Get.class).in(Singleton.class);
        bind(EnterpriseHandlers.Update.class).in(Singleton.class);
        bind(EnterpriseHandlers.Delete.class).in(Singleton.class);
        bind(EnterpriseHandlers.List.class).in(Singleton.class);

        // ── User handlers ─────────────────────────────────────────────────────
        bind(UserHandlers.Create.class).in(Singleton.class);
        bind(UserHandlers.Get.class).in(Singleton.class);
        bind(UserHandlers.Update.class).in(Singleton.class);
        bind(UserHandlers.Delete.class).in(Singleton.class);
        bind(UserHandlers.List.class).in(Singleton.class);
        bind(UserHandlers.GetUsersInGroup.class).in(Singleton.class);

        // ── Group handlers ────────────────────────────────────────────────────
        bind(GroupHandlers.Create.class).in(Singleton.class);
        bind(GroupHandlers.Get.class).in(Singleton.class);
        bind(GroupHandlers.Update.class).in(Singleton.class);
        bind(GroupHandlers.Delete.class).in(Singleton.class);
        bind(GroupHandlers.List.class).in(Singleton.class);
        bind(GroupHandlers.GetUserGroups.class).in(Singleton.class);

        // ── Role handlers ─────────────────────────────────────────────────────
        bind(RoleHandlers.Create.class).in(Singleton.class);
        bind(RoleHandlers.Get.class).in(Singleton.class);
        bind(RoleHandlers.Update.class).in(Singleton.class);
        bind(RoleHandlers.Delete.class).in(Singleton.class);
        bind(RoleHandlers.List.class).in(Singleton.class);
        bind(RoleHandlers.GetUserRoles.class).in(Singleton.class);

        // ── ApiKey handlers ───────────────────────────────────────────────────
        bind(ApiKeyHandlers.Create.class).in(Singleton.class);
        bind(ApiKeyHandlers.Get.class).in(Singleton.class);
        bind(ApiKeyHandlers.Revoke.class).in(Singleton.class);
        bind(ApiKeyHandlers.List.class).in(Singleton.class);

        // ── GroupMember handlers ──────────────────────────────────────────────
        bind(GroupMemberHandlers.Add.class).in(Singleton.class);
        bind(GroupMemberHandlers.Remove.class).in(Singleton.class);
        bind(GroupMemberHandlers.ListByGroup.class).in(Singleton.class);
        bind(GroupMemberHandlers.IsUserInGroup.class).in(Singleton.class);

        // ── UserRole handlers ─────────────────────────────────────────────────
        bind(UserRoleHandlers.Grant.class).in(Singleton.class);
        bind(UserRoleHandlers.Revoke.class).in(Singleton.class);
        bind(UserRoleHandlers.ListByUser.class).in(Singleton.class);
        bind(UserRoleHandlers.HasRole.class).in(Singleton.class);

        // ── GroupRole handlers ────────────────────────────────────────────────
        bind(GroupRoleHandlers.Assign.class).in(Singleton.class);
        bind(GroupRoleHandlers.Unassign.class).in(Singleton.class);
        bind(GroupRoleHandlers.ListByGroup.class).in(Singleton.class);

        // ── RolePermission handlers ───────────────────────────────────────────
        bind(RolePermissionHandlers.Add.class).in(Singleton.class);
        bind(RolePermissionHandlers.Remove.class).in(Singleton.class);
        bind(RolePermissionHandlers.ListByRole.class).in(Singleton.class);
        bind(RolePermissionHandlers.HasPermission.class).in(Singleton.class);
        bind(RolePermissionHandlers.GetUserPermissions.class).in(Singleton.class);
    }

    @Override
    public CompletableFuture<Boolean> start(long timeout, TimeUnit unit) {
        return CompletableFuture.completedFuture(true);
    }

    @Override
    public CompletableFuture<Boolean> shutdown(long timeout, TimeUnit unit) {
        return CompletableFuture.completedFuture(true);
    }

    // ── @Provides for service-container classes without @Inject constructors ──

    @Provides @Singleton
    LoginHandler provideLoginHandler(OAuthClient oAuthClient) {
        return new LoginHandler(oAuthClient);
    }

    @Provides @Singleton
    AuthGrpcService provideAuthGrpcService(LoginHandler loginHandler) {
        return new AuthGrpcService(loginHandler);
    }

    static final class ApiKeyResolverInstaller {
        @com.google.inject.Inject
        ApiKeyResolverInstaller(ApiKeyResolver resolver) {
            BaseServiceHandler.setApiKeyResolver(resolver);
        }
    }

    static final class UserContextResolverInstaller {
        @com.google.inject.Inject
        UserContextResolverInstaller(UserContextResolver resolver) {
            BaseServiceHandler.setUserContextResolver(resolver);
        }
    }
}
