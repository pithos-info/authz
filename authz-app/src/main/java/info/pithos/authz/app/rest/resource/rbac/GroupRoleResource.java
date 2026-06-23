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

package info.pithos.authz.app.rest.resource.rbac;

import com.google.inject.Inject;
import info.pithos.authz.app.handler.rbac.GroupRoleHandlers;
import info.pithos.rbac.service.AssignGroupRoleRequest;
import info.pithos.rbac.service.GetByIdRequest;
import info.pithos.rbac.service.UnassignGroupRoleRequest;
import info.pithos.service.container.core.BaseServiceHandler;
import io.vertx.ext.web.Router;

public final class GroupRoleResource {

    private final GroupRoleHandlers.Assign      assign;
    private final GroupRoleHandlers.Unassign    unassign;
    private final GroupRoleHandlers.ListByGroup listByGroup;

    @Inject
    public GroupRoleResource(
            GroupRoleHandlers.Assign      assign,
            GroupRoleHandlers.Unassign    unassign,
            GroupRoleHandlers.ListByGroup listByGroup) {
        this.assign      = assign;
        this.unassign    = unassign;
        this.listByGroup = listByGroup;
    }

    public void mount(Router r) {
        r.post("/groups/:groupId/roles").handler(ctx -> {
            AssignGroupRoleRequest req = BaseServiceHandler.parseBody(ctx, AssignGroupRoleRequest.newBuilder());
            if (req == null) return;
            BaseServiceHandler.route(ctx, 201, assign,
                req.toBuilder().setGroupId(ctx.pathParam("groupId")).build());
        });

        r.delete("/groups/:groupId/roles/:roleId").handler(ctx ->
            BaseServiceHandler.routeNoContent(ctx, unassign,
                UnassignGroupRoleRequest.newBuilder()
                    .setGroupId(ctx.pathParam("groupId"))
                    .setRoleId(ctx.pathParam("roleId"))
                    .build()));

        r.get("/groups/:groupId/roles").handler(ctx ->
            BaseServiceHandler.route(ctx, 200, listByGroup,
                GetByIdRequest.newBuilder().setId(ctx.pathParam("groupId")).build()));
    }
}
