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

package info.pithos.rbac.app.server;

import com.google.inject.Inject;
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
import info.pithos.service.container.core.grpc.AuthGrpcService;
import io.grpc.Server;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public final class RbacGrpcServer {

    private static final Logger log = LoggerFactory.getLogger(RbacGrpcServer.class);

    private final RbacServerConfig config;
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
    private final RolePermissionGrpcService rolePermissionGrpcService;

    private Server server;

    @Inject
    public RbacGrpcServer(
            RbacServerConfig config,
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
            RolePermissionGrpcService rolePermissionGrpcService) {
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
        this.rolePermissionGrpcService = rolePermissionGrpcService;
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
                .build()
                .start();
            log.info("gRPC server listening on :{}", config.grpcPort());
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
