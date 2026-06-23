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
import info.pithos.rbac.service.CreateGroupRequest;
import info.pithos.rbac.service.DeleteByIdRequest;
import info.pithos.rbac.service.Empty;
import info.pithos.rbac.service.GetByIdRequest;
import info.pithos.rbac.service.Group;
import info.pithos.rbac.service.GroupList;
import info.pithos.rbac.service.UpdateGroupRequest;
import info.pithos.runtime.core.context.ErrorCode;
import info.pithos.runtime.core.context.ServiceException;
import info.pithos.runtime.model.protocol.Context.RequestContext;
import info.pithos.serde.ProtoBufMapper;
import info.pithos.service.container.core.BaseServiceHandler;
import io.smallrye.mutiny.Uni;

import java.util.concurrent.CompletableFuture;

public final class GroupHandlers {

    private GroupHandlers() {}

    public static final class Create extends BaseServiceHandler<CreateGroupRequest, Group> {
        private final GroupService service;

        @Inject
        public Create(OAuthClient oAuthClient, GroupService service) {
            super(oAuthClient);
            this.service = service;
        }

        @Override
        public Uni<Group> handle(CreateGroupRequest req, RequestContext rc) {
            info.pithos.rbac.model.Rbac.Group data = info.pithos.rbac.model.Rbac.Group.newBuilder()
                .setEnterpriseId(rc.getAuthContext().getEnterpriseId())
                .setName(req.getName())
                .build();
            return Uni.createFrom().completionStage(() -> service.create(rc, data))
                .map(created -> ProtoBufMapper.map(created, Group.newBuilder()));
        }
    }

    public static final class Get extends BaseServiceHandler<GetByIdRequest, Group> {
        private final GroupService          groupService;
        private final GroupRoleService      groupRoleService;
        private final RoleService           roleService;
        private final RolePermissionService rpService;

        @Inject
        public Get(OAuthClient oAuthClient,
                   GroupService          groupService,
                   GroupRoleService      groupRoleService,
                   RoleService           roleService,
                   RolePermissionService rpService) {
            super(oAuthClient);
            this.groupService     = groupService;
            this.groupRoleService = groupRoleService;
            this.roleService      = roleService;
            this.rpService        = rpService;
        }

        @Override
        public Uni<Group> handle(GetByIdRequest req, RequestContext rc) {
            CompletableFuture<Group> future =
                groupService.get(rc, req.getId())
                    .thenApply(opt -> opt.orElseThrow(() ->
                        new ServiceException(ErrorCode.NOT_FOUND, "Group not found: " + req.getId())))
                    .thenCompose(data ->
                        RbacEnricher.apiGroup(rc, data, groupRoleService, roleService, rpService));
            return Uni.createFrom().completionStage(() -> future);
        }
    }

    public static final class Update extends BaseServiceHandler<UpdateGroupRequest, Group> {
        private final GroupService service;

        @Inject
        public Update(OAuthClient oAuthClient, GroupService service) {
            super(oAuthClient);
            this.service = service;
        }

        @Override
        public Uni<Group> handle(UpdateGroupRequest req, RequestContext rc) {
            info.pithos.rbac.model.Rbac.Group data = info.pithos.rbac.model.Rbac.Group.newBuilder()
                .setId(req.getId())
                .setName(req.getName())
                .build();
            return Uni.createFrom().completionStage(() -> service.update(rc, data))
                .map(updated -> ProtoBufMapper.map(updated, Group.newBuilder()));
        }
    }

    public static final class Delete extends BaseServiceHandler<DeleteByIdRequest, Empty> {
        private final GroupService service;

        @Inject
        public Delete(OAuthClient oAuthClient, GroupService service) {
            super(oAuthClient);
            this.service = service;
        }

        @Override
        public Uni<Empty> handle(DeleteByIdRequest req, RequestContext rc) {
            return Uni.createFrom().completionStage(() -> service.delete(rc, req.getId()))
                .map(v -> Empty.getDefaultInstance());
        }
    }

    public static final class List extends BaseServiceHandler<Empty, GroupList> {
        private final GroupService service;

        @Inject
        public List(OAuthClient oAuthClient, GroupService service) {
            super(oAuthClient);
            this.service = service;
        }

        @Override
        public Uni<GroupList> handle(Empty req, RequestContext rc) {
            return Uni.createFrom().completionStage(() -> service.list(rc))
                .map(items -> GroupList.newBuilder()
                    .addAllGroups(items.stream()
                        .map(g -> ProtoBufMapper.<Group>map(g, Group.newBuilder()))
                        .toList())
                    .build());
        }
    }

    public static final class GetUserGroups extends BaseServiceHandler<Empty, GroupList> {
        private final GroupService          groupService;
        private final GroupRoleService      groupRoleService;
        private final RoleService           roleService;
        private final RolePermissionService rpService;

        @Inject
        public GetUserGroups(OAuthClient oAuthClient,
                             GroupService          groupService,
                             GroupRoleService      groupRoleService,
                             RoleService           roleService,
                             RolePermissionService rpService) {
            super(oAuthClient);
            this.groupService     = groupService;
            this.groupRoleService = groupRoleService;
            this.roleService      = roleService;
            this.rpService        = rpService;
        }

        @Override
        public Uni<GroupList> handle(Empty req, RequestContext rc) {
            CompletableFuture<GroupList> future =
                groupService.getUserGroups(rc)
                    .thenCompose(gs -> RbacEnricher.apiGroups(rc, gs, groupRoleService, roleService, rpService))
                    .thenApply(groups -> GroupList.newBuilder().addAllGroups(groups).build());
            return Uni.createFrom().completionStage(() -> future);
        }
    }
}
