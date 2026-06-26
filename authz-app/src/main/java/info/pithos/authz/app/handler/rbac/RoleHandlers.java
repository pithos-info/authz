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

package info.pithos.authz.app.handler.rbac;

import com.google.inject.Inject;
import info.pithos.authn.OAuthClient;
import info.pithos.runtime.core.context.ApplicationContext;
import info.pithos.rbac.RolePermissionService;
import info.pithos.rbac.RoleService;
import info.pithos.rbac.model.Rbac;
import info.pithos.rbac.service.CreateRoleRequest;
import info.pithos.rbac.service.DeleteByIdRequest;
import info.pithos.rbac.service.Empty;
import info.pithos.rbac.service.GetByIdRequest;
import info.pithos.rbac.service.Role;
import info.pithos.rbac.service.RoleList;
import info.pithos.rbac.service.UpdateRoleRequest;
import info.pithos.runtime.core.context.ErrorCode;
import info.pithos.runtime.core.context.ServiceException;
import info.pithos.runtime.model.protocol.Context.RequestContext;
import info.pithos.serde.ProtoBufMapper;
import info.pithos.service.container.core.BaseServiceHandler;
import io.smallrye.mutiny.Uni;

import java.util.concurrent.CompletableFuture;

public final class RoleHandlers {

    private RoleHandlers() {}

    public static final class Create extends BaseServiceHandler<CreateRoleRequest, Role> {
        private final RoleService service;

        @Inject
        public Create(ApplicationContext applicationContext, OAuthClient oAuthClient, RoleService service) {
            super(applicationContext, oAuthClient);
            this.service = service;
        }

        @Override
        public Uni<Role> handle(CreateRoleRequest req, RequestContext rc) {
            Rbac.Role data = Rbac.Role.newBuilder()
                .setEnterpriseId(rc.getAuthContext().getEnterpriseId())
                .setName(req.getName())
                .build();
            return Uni.createFrom().completionStage(() -> service.create(rc, data))
                .map(created -> ProtoBufMapper.map(created, Role.newBuilder()));
        }
    }

    public static final class Get extends BaseServiceHandler<GetByIdRequest, Role> {
        private final RoleService           roleService;
        private final RolePermissionService rpService;

        @Inject
        public Get(ApplicationContext applicationContext, OAuthClient oAuthClient, RoleService roleService, RolePermissionService rpService) {
            super(applicationContext, oAuthClient);
            this.roleService = roleService;
            this.rpService   = rpService;
        }

        @Override
        public Uni<Role> handle(GetByIdRequest req, RequestContext rc) {
            CompletableFuture<Role> future =
                roleService.get(rc, req.getId())
                    .thenApply(opt -> opt.orElseThrow(() ->
                        new ServiceException(ErrorCode.NOT_FOUND, "Role not found: " + req.getId())))
                    .thenCompose(data -> RbacEnricher.apiRole(rc, data, rpService));
            return Uni.createFrom().completionStage(() -> future);
        }
    }

    public static final class Update extends BaseServiceHandler<UpdateRoleRequest, Role> {
        private final RoleService service;

        @Inject
        public Update(ApplicationContext applicationContext, OAuthClient oAuthClient, RoleService service) {
            super(applicationContext, oAuthClient);
            this.service = service;
        }

        @Override
        public Uni<Role> handle(UpdateRoleRequest req, RequestContext rc) {
            Rbac.Role data = Rbac.Role.newBuilder()
                .setId(req.getId())
                .setName(req.getName())
                .build();
            return Uni.createFrom().completionStage(() -> service.update(rc, data))
                .map(updated -> ProtoBufMapper.map(updated, Role.newBuilder()));
        }
    }

    public static final class Delete extends BaseServiceHandler<DeleteByIdRequest, Empty> {
        private final RoleService service;

        @Inject
        public Delete(ApplicationContext applicationContext, OAuthClient oAuthClient, RoleService service) {
            super(applicationContext, oAuthClient);
            this.service = service;
        }

        @Override
        public Uni<Empty> handle(DeleteByIdRequest req, RequestContext rc) {
            return Uni.createFrom().completionStage(() -> service.delete(rc, req.getId()))
                .map(v -> Empty.getDefaultInstance());
        }
    }

    public static final class List extends BaseServiceHandler<Empty, RoleList> {
        private final RoleService service;

        @Inject
        public List(ApplicationContext applicationContext, OAuthClient oAuthClient, RoleService service) {
            super(applicationContext, oAuthClient);
            this.service = service;
        }

        @Override
        public Uni<RoleList> handle(Empty req, RequestContext rc) {
            return Uni.createFrom().completionStage(() -> service.list(rc))
                .map(items -> RoleList.newBuilder()
                    .addAllRoles(items.stream()
                        .map(r -> ProtoBufMapper.<Role>map(r, Role.newBuilder()))
                        .toList())
                    .build());
        }
    }

    public static final class GetUserRoles extends BaseServiceHandler<Empty, RoleList> {
        private final RoleService           roleService;
        private final RolePermissionService rpService;

        @Inject
        public GetUserRoles(ApplicationContext applicationContext, OAuthClient oAuthClient, RoleService roleService, RolePermissionService rpService) {
            super(applicationContext, oAuthClient);
            this.roleService = roleService;
            this.rpService   = rpService;
        }

        @Override
        public Uni<RoleList> handle(Empty req, RequestContext rc) {
            CompletableFuture<RoleList> future =
                roleService.getUserRoles(rc)
                    .thenCompose(rs -> RbacEnricher.apiRoles(rc, rs, rpService))
                    .thenApply(roles -> RoleList.newBuilder().addAllRoles(roles).build());
            return Uni.createFrom().completionStage(() -> future);
        }
    }
}
