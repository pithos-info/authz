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
import info.pithos.authz.app.handler.rbac.UserRoleHandlers;
import info.pithos.rbac.service.GetByIdRequest;
import info.pithos.rbac.service.GrantUserRoleRequest;
import info.pithos.rbac.service.RevokeUserRoleRequest;
import info.pithos.service.container.core.RouteHelper;
import io.vertx.ext.web.Router;

public final class UserRoleResource {

    private final RouteHelper routeHelper;

    private final UserRoleHandlers.Grant      grant;
    private final UserRoleHandlers.Revoke     revoke;
    private final UserRoleHandlers.ListByUser listByUser;

    @Inject
    public UserRoleResource(RouteHelper routeHelper,
            UserRoleHandlers.Grant      grant,
            UserRoleHandlers.Revoke     revoke,
            UserRoleHandlers.ListByUser listByUser) {
        this.routeHelper = routeHelper;
        this.grant      = grant;
        this.revoke     = revoke;
        this.listByUser = listByUser;
    }

    public void mount(Router r) {
        r.post("/users/:userId/roles").handler(ctx -> {
            GrantUserRoleRequest req = routeHelper.parseBody(ctx, GrantUserRoleRequest.newBuilder());
            if (req == null) return;
            routeHelper.route(ctx, 201, grant,
                req.toBuilder().setUserId(ctx.pathParam("userId")).build());
        });

        r.delete("/users/:userId/roles/:roleId").handler(ctx ->
            routeHelper.routeNoContent(ctx, revoke,
                RevokeUserRoleRequest.newBuilder()
                    .setUserId(ctx.pathParam("userId"))
                    .setRoleId(ctx.pathParam("roleId"))
                    .build()));

        r.get("/users/:userId/roles").handler(ctx ->
            routeHelper.route(ctx, 200, listByUser,
                GetByIdRequest.newBuilder().setId(ctx.pathParam("userId")).build()));
    }
}
