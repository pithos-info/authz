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
import info.pithos.authz.app.handler.rbac.RolePermissionHandlers;
import info.pithos.rbac.service.AddRolePermissionRequest;
import info.pithos.rbac.service.GetByIdRequest;
import info.pithos.rbac.service.RemoveRolePermissionRequest;
import info.pithos.service.container.core.RouteHelper;
import io.vertx.ext.web.Router;

public final class RolePermissionResource {

    private final RouteHelper routeHelper;

    private final RolePermissionHandlers.Add      add;
    private final RolePermissionHandlers.Remove   remove;
    private final RolePermissionHandlers.ListByRole listByRole;

    @Inject
    public RolePermissionResource(RouteHelper routeHelper,
            RolePermissionHandlers.Add      add,
            RolePermissionHandlers.Remove   remove,
            RolePermissionHandlers.ListByRole listByRole) {
        this.routeHelper = routeHelper;
        this.add        = add;
        this.remove     = remove;
        this.listByRole = listByRole;
    }

    public void mount(Router r) {
        r.post("/roles/:roleId/permissions").handler(ctx -> {
            AddRolePermissionRequest req = routeHelper.parseBody(ctx, AddRolePermissionRequest.newBuilder());
            if (req == null) return;
            routeHelper.route(ctx, 201, add,
                req.toBuilder().setRoleId(ctx.pathParam("roleId")).build());
        });

        r.delete("/roles/:roleId/permissions/:permission").handler(ctx ->
            routeHelper.routeNoContent(ctx, remove,
                RemoveRolePermissionRequest.newBuilder()
                    .setRoleId(ctx.pathParam("roleId"))
                    .setPermission(ctx.pathParam("permission"))
                    .build()));

        r.get("/roles/:roleId/permissions").handler(ctx ->
            routeHelper.route(ctx, 200, listByRole,
                GetByIdRequest.newBuilder().setId(ctx.pathParam("roleId")).build()));
    }
}
