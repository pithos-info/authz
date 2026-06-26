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

package info.pithos.authz.app.server;

import com.google.inject.Inject;
import info.pithos.authz.app.config.AuthZServerConfig;
import info.pithos.authz.app.grpc.monetization.AppGrpcService;
import info.pithos.authz.app.grpc.rbac.ApiKeyGrpcService;
import info.pithos.authz.app.grpc.rbac.EnterpriseGrpcService;
import info.pithos.authz.app.grpc.monetization.FeatureGrpcService;
import info.pithos.authz.app.grpc.monetization.JourneyGrpcService;
import info.pithos.authz.app.grpc.monetization.WorkflowFeatureGrpcService;
import info.pithos.authz.app.grpc.monetization.WorkflowGrpcService;
import info.pithos.authz.app.grpc.rbac.GroupGrpcService;
import info.pithos.authz.app.grpc.rbac.GroupMemberGrpcService;
import info.pithos.authz.app.grpc.rbac.GroupRoleGrpcService;
import info.pithos.service.container.core.grpc.GrpcMetadataInterceptor;
import info.pithos.authz.app.grpc.rbac.RoleGrpcService;
import info.pithos.authz.app.grpc.rbac.RolePermissionGrpcService;
import info.pithos.authz.app.grpc.rbac.UserGrpcService;
import info.pithos.authz.app.grpc.rbac.UserRoleGrpcService;
import info.pithos.service.container.core.grpc.AuthGrpcService;
import io.grpc.Server;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;
import info.pithos.runtime.core.context.ApplicationContext;
import info.pithos.runtime.model.protocol.Context.LogLevelType;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public final class AuthZGrpcServer {

    private final ApplicationContext applicationContext;
    private final AuthZServerConfig config;
    private final GrpcMetadataInterceptor interceptor;
    private final AuthGrpcService authGrpcService;
    private final EnterpriseGrpcService enterpriseGrpcService;
    private final UserGrpcService userGrpcService;
    private final GroupGrpcService groupGrpcService;
    private final RoleGrpcService roleGrpcService;
    private final ApiKeyGrpcService apiKeyGrpcService;
    private final GroupMemberGrpcService groupMemberGrpcService;
    private final UserRoleGrpcService userRoleGrpcService;
    private final GroupRoleGrpcService groupRoleGrpcService;
    private final RolePermissionGrpcService  rolePermissionGrpcService;
    private final AppGrpcService             appGrpcService;
    private final FeatureGrpcService         featureGrpcService;
    private final JourneyGrpcService         journeyGrpcService;
    private final WorkflowGrpcService        workflowGrpcService;
    private final WorkflowFeatureGrpcService workflowFeatureGrpcService;

    private Server server;

    @Inject
    public AuthZGrpcServer(
            ApplicationContext applicationContext,
            AuthZServerConfig config,
            GrpcMetadataInterceptor interceptor,
            AuthGrpcService authGrpcService,
            EnterpriseGrpcService enterpriseGrpcService,
            UserGrpcService userGrpcService,
            GroupGrpcService groupGrpcService,
            RoleGrpcService roleGrpcService,
            ApiKeyGrpcService apiKeyGrpcService,
            GroupMemberGrpcService groupMemberGrpcService,
            UserRoleGrpcService userRoleGrpcService,
            GroupRoleGrpcService groupRoleGrpcService,
            RolePermissionGrpcService  rolePermissionGrpcService,
            AppGrpcService             appGrpcService,
            FeatureGrpcService         featureGrpcService,
            JourneyGrpcService         journeyGrpcService,
            WorkflowGrpcService        workflowGrpcService,
            WorkflowFeatureGrpcService workflowFeatureGrpcService) {
        this.applicationContext = applicationContext;
        this.config = config;
        this.interceptor = interceptor;
        this.authGrpcService = authGrpcService;
        this.enterpriseGrpcService = enterpriseGrpcService;
        this.userGrpcService = userGrpcService;
        this.groupGrpcService = groupGrpcService;
        this.roleGrpcService = roleGrpcService;
        this.apiKeyGrpcService = apiKeyGrpcService;
        this.groupMemberGrpcService = groupMemberGrpcService;
        this.userRoleGrpcService = userRoleGrpcService;
        this.groupRoleGrpcService = groupRoleGrpcService;
        this.rolePermissionGrpcService  = rolePermissionGrpcService;
        this.appGrpcService             = appGrpcService;
        this.featureGrpcService         = featureGrpcService;
        this.journeyGrpcService         = journeyGrpcService;
        this.workflowGrpcService        = workflowGrpcService;
        this.workflowFeatureGrpcService = workflowFeatureGrpcService;
    }

    public void start() {
        try {
            server = NettyServerBuilder.forPort(config.grpcPort())
                .intercept(interceptor)
                .addService(authGrpcService)
                .addService(enterpriseGrpcService)
                .addService(userGrpcService)
                .addService(groupGrpcService)
                .addService(roleGrpcService)
                .addService(apiKeyGrpcService)
                .addService(groupMemberGrpcService)
                .addService(userRoleGrpcService)
                .addService(groupRoleGrpcService)
                .addService(rolePermissionGrpcService)
                .addService(appGrpcService)
                .addService(featureGrpcService)
                .addService(journeyGrpcService)
                .addService(workflowGrpcService)
                .addService(workflowFeatureGrpcService)
                .build()
                .start();
            applicationContext.getSystemContext().getLogger()
                .logRequest(null, AuthZGrpcServer.class, LogLevelType.INFO, "gRPC server listening on :{}", config.grpcPort());
        } catch (IOException e) {
            throw new IllegalStateException("Failed to start gRPC server on port " + config.grpcPort(), e);
        }
    }

    public void stop() {
        if (server != null) {
            server.shutdown();
            try {
                server.awaitTermination(10, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                server.shutdownNow();
            }
        }
    }
}
