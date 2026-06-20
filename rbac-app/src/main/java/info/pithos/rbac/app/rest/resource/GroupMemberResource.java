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

package info.pithos.rbac.app.rest.resource;

import com.google.inject.Inject;
import info.pithos.rbac.app.handler.GroupMemberHandlers;
import info.pithos.rbac.service.AddGroupMemberRequest;
import info.pithos.rbac.service.GetByIdRequest;
import info.pithos.rbac.service.RemoveGroupMemberRequest;
import info.pithos.service.container.core.BaseServiceHandler;
import io.vertx.ext.web.Router;

public final class GroupMemberResource {

    private final GroupMemberHandlers.Add          add;
    private final GroupMemberHandlers.Remove       remove;
    private final GroupMemberHandlers.ListByGroup  listByGroup;
    private final GroupMemberHandlers.IsUserInGroup isUserInGroup;

    @Inject
    public GroupMemberResource(
            GroupMemberHandlers.Add          add,
            GroupMemberHandlers.Remove       remove,
            GroupMemberHandlers.ListByGroup  listByGroup,
            GroupMemberHandlers.IsUserInGroup isUserInGroup) {
        this.add          = add;
        this.remove       = remove;
        this.listByGroup  = listByGroup;
        this.isUserInGroup = isUserInGroup;
    }

    public void mount(Router r) {
        r.post("/groups/:groupId/members").handler(ctx -> {
            AddGroupMemberRequest req = BaseServiceHandler.parseBody(ctx, AddGroupMemberRequest.newBuilder());
            if (req == null) return;
            BaseServiceHandler.route(ctx, 201, add,
                req.toBuilder().setGroupId(ctx.pathParam("groupId")).build());
        });

        r.delete("/groups/:groupId/members/:userId").handler(ctx ->
            BaseServiceHandler.routeNoContent(ctx, remove,
                RemoveGroupMemberRequest.newBuilder()
                    .setGroupId(ctx.pathParam("groupId"))
                    .setUserId(ctx.pathParam("userId"))
                    .build()));

        r.get("/groups/:groupId/members").handler(ctx ->
            BaseServiceHandler.route(ctx, 200, listByGroup,
                GetByIdRequest.newBuilder().setId(ctx.pathParam("groupId")).build()));

        // checks whether the authenticated user (from X-User-Id / token) is a member
        r.get("/groups/:groupId/me").handler(ctx ->
            BaseServiceHandler.route(ctx, 200, isUserInGroup,
                GetByIdRequest.newBuilder().setId(ctx.pathParam("groupId")).build()));
    }
}
