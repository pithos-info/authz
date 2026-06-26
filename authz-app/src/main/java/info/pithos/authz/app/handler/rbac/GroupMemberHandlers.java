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
import info.pithos.rbac.GroupMemberService;
import info.pithos.data.relational.FilterCriteria;
import info.pithos.rbac.service.AddGroupMemberRequest;
import info.pithos.rbac.service.Bool;
import info.pithos.rbac.service.Empty;
import info.pithos.rbac.service.GetByIdRequest;
import info.pithos.rbac.service.GroupMember;
import info.pithos.rbac.service.GroupMemberList;
import info.pithos.rbac.service.RemoveGroupMemberRequest;
import info.pithos.runtime.model.protocol.Context.RequestContext;
import info.pithos.serde.ProtoBufMapper;
import info.pithos.service.container.core.BaseServiceHandler;
import io.smallrye.mutiny.Uni;


public final class GroupMemberHandlers {

    private GroupMemberHandlers() {}

    public static final class Add extends BaseServiceHandler<AddGroupMemberRequest, GroupMember> {
        private final GroupMemberService service;

        @Inject
        public Add(ApplicationContext applicationContext, OAuthClient oAuthClient, GroupMemberService service) {
            super(applicationContext, oAuthClient);
            this.service = service;
        }

        @Override
        public Uni<GroupMember> handle(AddGroupMemberRequest req, RequestContext rc) {
            return Uni.createFrom()
                .completionStage(() -> service.add(rc, req.getGroupId(), req.getUserId()))
                .map(created -> ProtoBufMapper.map(created, GroupMember.newBuilder()));
        }
    }

    public static final class Remove extends BaseServiceHandler<RemoveGroupMemberRequest, Empty> {
        private final GroupMemberService service;

        @Inject
        public Remove(ApplicationContext applicationContext, OAuthClient oAuthClient, GroupMemberService service) {
            super(applicationContext, oAuthClient);
            this.service = service;
        }

        @Override
        public Uni<Empty> handle(RemoveGroupMemberRequest req, RequestContext rc) {
            return Uni.createFrom()
                .completionStage(() -> service.remove(rc, req.getGroupId(), req.getUserId()))
                .map(v -> Empty.getDefaultInstance());
        }
    }

    public static final class ListByGroup extends BaseServiceHandler<GetByIdRequest, GroupMemberList> {
        private final GroupMemberService service;

        @Inject
        public ListByGroup(ApplicationContext applicationContext, OAuthClient oAuthClient, GroupMemberService service) {
            super(applicationContext, oAuthClient);
            this.service = service;
        }

        @Override
        public Uni<GroupMemberList> handle(GetByIdRequest req, RequestContext rc) {
            return Uni.createFrom()
                .completionStage(() -> service.select(rc,
                    FilterCriteria.eq("groupId", req.getId())))
                .map(items -> GroupMemberList.newBuilder()
                    .addAllMembers(items.stream()
                        .map(m -> ProtoBufMapper.<GroupMember>map(m, GroupMember.newBuilder()))
                        .toList())
                    .build());
        }
    }

    public static final class IsUserInGroup extends BaseServiceHandler<GetByIdRequest, Bool> {
        private final GroupMemberService service;

        @Inject
        public IsUserInGroup(ApplicationContext applicationContext, OAuthClient oAuthClient, GroupMemberService service) {
            super(applicationContext, oAuthClient);
            this.service = service;
        }

        @Override
        public Uni<Bool> handle(GetByIdRequest req, RequestContext rc) {
            return Uni.createFrom()
                .completionStage(() -> service.isUserInGroup(rc, req.getId()))
                .map(result -> Bool.newBuilder().setValue(result).build());
        }
    }
}
