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

package info.pithos.authz.app;

import com.google.inject.Provides;
import com.google.inject.Singleton;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import info.pithos.authn.OAuthClient;
import info.pithos.authz.app.config.AuthZServerConfig;
import info.pithos.runtime.core.context.ApplicationContext;
import info.pithos.authz.app.grpc.rbac.ApiKeyGrpcService;
import info.pithos.authz.app.grpc.rbac.EnterpriseGrpcService;
import info.pithos.authz.app.grpc.rbac.GroupGrpcService;
import info.pithos.authz.app.grpc.rbac.GroupMemberGrpcService;
import info.pithos.authz.app.grpc.rbac.GroupRoleGrpcService;
import info.pithos.service.container.core.grpc.GrpcMetadataInterceptor;
import info.pithos.authz.app.grpc.rbac.RoleGrpcService;
import info.pithos.authz.app.grpc.rbac.RolePermissionGrpcService;
import info.pithos.authz.app.grpc.rbac.UserGrpcService;
import info.pithos.authz.app.grpc.rbac.UserRoleGrpcService;
import info.pithos.authz.app.handler.rbac.ApiKeyHandlers;
import info.pithos.authz.app.handler.rbac.EnterpriseHandlers;
import info.pithos.authz.app.handler.rbac.GroupHandlers;
import info.pithos.authz.app.handler.rbac.GroupMemberHandlers;
import info.pithos.authz.app.handler.rbac.GroupRoleHandlers;
import info.pithos.authz.app.handler.rbac.RoleHandlers;
import info.pithos.authz.app.handler.rbac.RolePermissionHandlers;
import info.pithos.authz.app.handler.rbac.UserHandlers;
import info.pithos.authz.app.handler.rbac.UserRoleHandlers;
import info.pithos.authz.app.auth.RbacApiKeyResolver;
import info.pithos.authz.app.auth.RbacUserContextResolver;
import info.pithos.authz.app.rest.AuthZRestRouter;
import info.pithos.authz.app.rest.resource.rbac.ApiKeyResource;
import info.pithos.authz.app.rest.resource.rbac.AuthResource;
import info.pithos.authz.app.rest.resource.rbac.EnterpriseResource;
import info.pithos.authz.app.rest.resource.rbac.GroupMemberResource;
import info.pithos.authz.app.rest.resource.rbac.GroupResource;
import info.pithos.authz.app.rest.resource.rbac.GroupRoleResource;
import info.pithos.authz.app.rest.resource.rbac.MeResource;
import info.pithos.authz.app.rest.resource.rbac.RolePermissionResource;
import info.pithos.authz.app.rest.resource.rbac.RoleResource;
import info.pithos.authz.app.rest.resource.rbac.UserResource;
import info.pithos.authz.app.rest.resource.rbac.UserRoleResource;
import info.pithos.monetization.service.AppService;
import info.pithos.monetization.service.FeatureService;
import info.pithos.monetization.service.JourneyService;
import info.pithos.monetization.service.WorkflowFeatureService;
import info.pithos.monetization.service.WorkflowService;
import info.pithos.authz.app.grpc.monetization.AppGrpcService;
import info.pithos.authz.app.grpc.monetization.FeatureGrpcService;
import info.pithos.authz.app.grpc.monetization.JourneyGrpcService;
import info.pithos.authz.app.grpc.monetization.WorkflowFeatureGrpcService;
import info.pithos.authz.app.grpc.monetization.WorkflowGrpcService;
import info.pithos.authz.app.handler.monetization.AppHandlers;
import info.pithos.authz.app.handler.monetization.FeatureHandlers;
import info.pithos.authz.app.handler.monetization.JourneyHandlers;
import info.pithos.authz.app.handler.monetization.WorkflowFeatureHandlers;
import info.pithos.authz.app.handler.monetization.WorkflowHandlers;
import info.pithos.authz.app.rest.resource.monetization.AppResource;
import info.pithos.authz.app.rest.resource.monetization.FeatureResource;
import info.pithos.authz.app.rest.resource.monetization.JourneyResource;
import info.pithos.authz.app.rest.resource.monetization.WorkflowFeatureResource;
import info.pithos.authz.app.rest.resource.monetization.WorkflowResource;
import info.pithos.authz.app.server.AuthZGrpcServer;
import info.pithos.authz.app.server.AuthZHttpServer;
import info.pithos.runtime.core.context.ApplicationContext;
import info.pithos.runtime.core.context.ServiceModule;
import info.pithos.service.container.core.auth.ApiKeyResolver;
import info.pithos.service.container.core.auth.UserContextResolver;
import info.pithos.service.container.core.BaseServiceHandler;
import info.pithos.service.container.core.LoginHandler;
import info.pithos.service.container.core.RouteHelper;
import info.pithos.service.container.core.grpc.AuthGrpcService;

final class AuthZAppModule extends ServiceModule {

    private final int httpPort;
    private final int grpcPort;
    private final boolean bypassAuth;

    AuthZAppModule(ApplicationContext context, int httpPort, int grpcPort, boolean bypassAuth) {
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
        bind(AuthZServerConfig.class).toInstance(new AuthZServerConfig(httpPort, grpcPort, bypassAuth));

        // ── Routing helper ────────────────────────────────────────────────────
        bind(RouteHelper.class).in(Singleton.class);

        // ── Servers & router ─────────────────────────────────────────────────
        bind(AuthZHttpServer.class).in(Singleton.class);
        bind(AuthZGrpcServer.class).in(Singleton.class);
        bind(AuthZRestRouter.class).in(Singleton.class);

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
        bind(AppResource.class).in(Singleton.class);
        bind(FeatureResource.class).in(Singleton.class);
        bind(JourneyResource.class).in(Singleton.class);
        bind(WorkflowResource.class).in(Singleton.class);
        bind(WorkflowFeatureResource.class).in(Singleton.class);

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
        bind(AppGrpcService.class).in(Singleton.class);
        bind(FeatureGrpcService.class).in(Singleton.class);
        bind(JourneyGrpcService.class).in(Singleton.class);
        bind(WorkflowGrpcService.class).in(Singleton.class);
        bind(WorkflowFeatureGrpcService.class).in(Singleton.class);
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

        // ── App handlers ──────────────────────────────────────────────────────
        bind(AppHandlers.Create.class).in(Singleton.class);
        bind(AppHandlers.Get.class).in(Singleton.class);
        bind(AppHandlers.List.class).in(Singleton.class);

        // ── Feature handlers ──────────────────────────────────────────────────
        bind(FeatureHandlers.Create.class).in(Singleton.class);
        bind(FeatureHandlers.Get.class).in(Singleton.class);
        bind(FeatureHandlers.ListByApp.class).in(Singleton.class);

        // ── Journey handlers ──────────────────────────────────────────────────
        bind(JourneyHandlers.Create.class).in(Singleton.class);
        bind(JourneyHandlers.Get.class).in(Singleton.class);
        bind(JourneyHandlers.ListByApp.class).in(Singleton.class);

        // ── Workflow handlers ─────────────────────────────────────────────────
        bind(WorkflowHandlers.Create.class).in(Singleton.class);
        bind(WorkflowHandlers.Get.class).in(Singleton.class);
        bind(WorkflowHandlers.ListByJourney.class).in(Singleton.class);

        // ── WorkflowFeature handlers ──────────────────────────────────────────
        bind(WorkflowFeatureHandlers.Add.class).in(Singleton.class);
        bind(WorkflowFeatureHandlers.Remove.class).in(Singleton.class);
        bind(WorkflowFeatureHandlers.ListByWorkflow.class).in(Singleton.class);
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
    ApplicationContext provideApplicationContext() {
        return getApplicationContext();
    }

    @Provides @Singleton
    LoginHandler provideLoginHandler(ApplicationContext applicationContext, OAuthClient oAuthClient) {
        return new LoginHandler(applicationContext, oAuthClient);
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
