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
import info.pithos.data.relational.FilterCriteria;
import info.pithos.rbac.GroupRoleService;
import info.pithos.rbac.service.AssignGroupRoleRequest;
import info.pithos.rbac.service.Empty;
import info.pithos.rbac.service.GetByIdRequest;
import info.pithos.rbac.service.GroupRole;
import info.pithos.rbac.service.GroupRoleList;
import info.pithos.rbac.service.UnassignGroupRoleRequest;
import info.pithos.runtime.model.protocol.Context.RequestContext;
import info.pithos.serde.ProtoBufMapper;
import info.pithos.service.container.core.BaseServiceHandler;
import io.smallrye.mutiny.Uni;


public final class GroupRoleHandlers {

    private GroupRoleHandlers() {}

    public static final class Assign extends BaseServiceHandler<AssignGroupRoleRequest, GroupRole> {
        private final GroupRoleService service;

        @Inject
        public Assign(ApplicationContext applicationContext, OAuthClient oAuthClient, GroupRoleService service) {
            super(applicationContext, oAuthClient);
            this.service = service;
        }

        @Override
        public Uni<GroupRole> handle(AssignGroupRoleRequest req, RequestContext rc) {
            return Uni.createFrom()
                .completionStage(() -> service.assign(rc, req.getGroupId(), req.getRoleId()))
                .map(created -> ProtoBufMapper.map(created, GroupRole.newBuilder()));
        }
    }

    public static final class Unassign extends BaseServiceHandler<UnassignGroupRoleRequest, Empty> {
        private final GroupRoleService service;

        @Inject
        public Unassign(ApplicationContext applicationContext, OAuthClient oAuthClient, GroupRoleService service) {
            super(applicationContext, oAuthClient);
            this.service = service;
        }

        @Override
        public Uni<Empty> handle(UnassignGroupRoleRequest req, RequestContext rc) {
            return Uni.createFrom()
                .completionStage(() -> service.unassign(rc, req.getGroupId(), req.getRoleId()))
                .map(v -> Empty.getDefaultInstance());
        }
    }

    public static final class ListByGroup extends BaseServiceHandler<GetByIdRequest, GroupRoleList> {
        private final GroupRoleService service;

        @Inject
        public ListByGroup(ApplicationContext applicationContext, OAuthClient oAuthClient, GroupRoleService service) {
            super(applicationContext, oAuthClient);
            this.service = service;
        }

        @Override
        public Uni<GroupRoleList> handle(GetByIdRequest req, RequestContext rc) {
            return Uni.createFrom()
                .completionStage(() -> service.select(rc,
                    FilterCriteria.eq("groupId", req.getId())))
                .map(items -> GroupRoleList.newBuilder()
                    .addAllGroupRoles(items.stream()
                        .map(gr -> ProtoBufMapper.<GroupRole>map(gr, GroupRole.newBuilder()))
                        .toList())
                    .build());
        }
    }
}
