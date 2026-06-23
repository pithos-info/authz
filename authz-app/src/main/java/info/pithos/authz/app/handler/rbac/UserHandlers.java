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
import info.pithos.auth.OAuthClient;
import info.pithos.rbac.GroupRoleService;
import info.pithos.rbac.GroupService;
import info.pithos.rbac.RolePermissionService;
import info.pithos.rbac.RoleService;
import info.pithos.rbac.UserService;
import info.pithos.rbac.model.Rbac;
import info.pithos.rbac.service.CreateUserRequest;
import info.pithos.rbac.service.DeleteByIdRequest;
import info.pithos.rbac.service.Empty;
import info.pithos.rbac.service.GetByIdRequest;
import info.pithos.rbac.service.UpdateUserRequest;
import info.pithos.rbac.service.User;
import info.pithos.rbac.service.UserList;
import info.pithos.runtime.core.context.ErrorCode;
import info.pithos.runtime.core.context.ServiceException;
import info.pithos.runtime.model.protocol.Context.RequestContext;
import info.pithos.serde.ProtoBufMapper;
import info.pithos.service.container.core.BaseServiceHandler;
import io.smallrye.mutiny.Uni;

import java.util.concurrent.CompletableFuture;

public final class UserHandlers {

    private UserHandlers() {}

    public static final class Create extends BaseServiceHandler<CreateUserRequest, User> {
        private final UserService service;

        @Inject
        public Create(OAuthClient oAuthClient, UserService service) {
            super(oAuthClient);
            this.service = service;
        }

        @Override
        public Uni<User> handle(CreateUserRequest req, RequestContext rc) {
            Rbac.User data = Rbac.User.newBuilder()
                .setEnterpriseId(req.getEnterpriseId())
                .setEmail(req.getEmail())
                .setExternalId(req.getExternalId())
                .setIdpProvider(req.getIdpProvider())
                .setDisplayName(req.getDisplayName())
                .build();
            return Uni.createFrom().completionStage(() -> service.create(rc, data))
                .map(created -> ProtoBufMapper.map(created, User.newBuilder()));
        }
    }

    public static final class Get extends BaseServiceHandler<GetByIdRequest, User> {
        private final UserService           userService;
        private final GroupService          groupService;
        private final GroupRoleService      groupRoleService;
        private final RoleService           roleService;
        private final RolePermissionService rpService;

        @Inject
        public Get(OAuthClient oAuthClient,
                   UserService           userService,
                   GroupService          groupService,
                   GroupRoleService      groupRoleService,
                   RoleService           roleService,
                   RolePermissionService rpService) {
            super(oAuthClient);
            this.userService      = userService;
            this.groupService     = groupService;
            this.groupRoleService = groupRoleService;
            this.roleService      = roleService;
            this.rpService        = rpService;
        }

        @Override
        public Uni<User> handle(GetByIdRequest req, RequestContext rc) {
            CompletableFuture<User> future =
                userService.get(rc, req.getId())
                    .thenApply(opt -> opt.orElseThrow(() ->
                        new ServiceException(ErrorCode.NOT_FOUND, "User not found: " + req.getId())))
                    .thenCompose(data -> {
                        // groups the user belongs to, each with their roles
                        CompletableFuture<java.util.List<info.pithos.rbac.service.Group>> groupsFuture =
                            groupService.getUserGroups(rc).thenCompose(gs ->
                                RbacEnricher.apiGroups(rc, gs, groupRoleService, roleService, rpService));
                        // effective roles (direct + group-inherited), each with permissions
                        CompletableFuture<java.util.List<info.pithos.rbac.service.Role>> rolesFuture =
                            roleService.getUserRoles(rc).thenCompose(rs ->
                                RbacEnricher.apiRoles(rc, rs, rpService));
                        return groupsFuture.thenCombine(rolesFuture, (groups, roles) ->
                            ProtoBufMapper.<User>map(data, User.newBuilder())
                                .toBuilder()
                                .addAllGroups(groups)
                                .addAllRoles(roles)
                                .build());
                    });
            return Uni.createFrom().completionStage(() -> future);
        }
    }

    public static final class Update extends BaseServiceHandler<UpdateUserRequest, User> {
        private final UserService service;

        @Inject
        public Update(OAuthClient oAuthClient, UserService service) {
            super(oAuthClient);
            this.service = service;
        }

        @Override
        public Uni<User> handle(UpdateUserRequest req, RequestContext rc) {
            Rbac.User data = Rbac.User.newBuilder()
                .setId(req.getId())
                .setDisplayName(req.getDisplayName())
                .build();
            return Uni.createFrom().completionStage(() -> service.update(rc, data))
                .map(updated -> ProtoBufMapper.map(updated, User.newBuilder()));
        }
    }

    public static final class Delete extends BaseServiceHandler<DeleteByIdRequest, Empty> {
        private final UserService service;

        @Inject
        public Delete(OAuthClient oAuthClient, UserService service) {
            super(oAuthClient);
            this.service = service;
        }

        @Override
        public Uni<Empty> handle(DeleteByIdRequest req, RequestContext rc) {
            return Uni.createFrom().completionStage(() -> service.delete(rc, req.getId()))
                .map(v -> Empty.getDefaultInstance());
        }
    }

    public static final class List extends BaseServiceHandler<Empty, UserList> {
        private final UserService service;

        @Inject
        public List(OAuthClient oAuthClient, UserService service) {
            super(oAuthClient);
            this.service = service;
        }

        @Override
        public Uni<UserList> handle(Empty req, RequestContext rc) {
            return Uni.createFrom().completionStage(() -> service.list(rc))
                .map(items -> UserList.newBuilder()
                    .addAllUsers(items.stream()
                        .map(u -> ProtoBufMapper.<User>map(u, User.newBuilder()))
                        .toList())
                    .build());
        }
    }

    public static final class GetUsersInGroup extends BaseServiceHandler<GetByIdRequest, UserList> {
        private final UserService service;

        @Inject
        public GetUsersInGroup(OAuthClient oAuthClient, UserService service) {
            super(oAuthClient);
            this.service = service;
        }

        @Override
        public Uni<UserList> handle(GetByIdRequest req, RequestContext rc) {
            return Uni.createFrom().completionStage(() -> service.getUsersInGroup(rc, req.getId()))
                .map(items -> UserList.newBuilder()
                    .addAllUsers(items.stream()
                        .map(u -> ProtoBufMapper.<User>map(u, User.newBuilder()))
                        .toList())
                    .build());
        }
    }
}
