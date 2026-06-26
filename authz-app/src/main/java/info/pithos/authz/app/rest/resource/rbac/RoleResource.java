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
import info.pithos.authz.app.handler.rbac.RoleHandlers;
import info.pithos.rbac.service.CreateRoleRequest;
import info.pithos.rbac.service.DeleteByIdRequest;
import info.pithos.rbac.service.Empty;
import info.pithos.rbac.service.GetByIdRequest;
import info.pithos.rbac.service.UpdateRoleRequest;
import info.pithos.service.container.core.RouteHelper;
import io.vertx.ext.web.Router;

public final class RoleResource {

    private final RouteHelper routeHelper;

    private final RoleHandlers.Create create;
    private final RoleHandlers.Get    get;
    private final RoleHandlers.Update update;
    private final RoleHandlers.Delete delete;
    private final RoleHandlers.List   list;

    @Inject
    public RoleResource(RouteHelper routeHelper,
            RoleHandlers.Create create,
            RoleHandlers.Get    get,
            RoleHandlers.Update update,
            RoleHandlers.Delete delete,
            RoleHandlers.List   list) {
        this.routeHelper = routeHelper;
        this.create = create;
        this.get    = get;
        this.update = update;
        this.delete = delete;
        this.list   = list;
    }

    public void mount(Router r) {
        r.get("/roles").handler(ctx ->
            routeHelper.route(ctx, 200, list, Empty.getDefaultInstance()));

        r.post("/roles").handler(ctx -> {
            CreateRoleRequest req = routeHelper.parseBody(ctx, CreateRoleRequest.newBuilder());
            if (req == null) return;
            routeHelper.route(ctx, 201, create, req);
        });

        r.get("/roles/:id").handler(ctx ->
            routeHelper.route(ctx, 200, get,
                GetByIdRequest.newBuilder().setId(ctx.pathParam("id")).build()));

        r.put("/roles/:id").handler(ctx -> {
            UpdateRoleRequest req = routeHelper.parseBody(ctx, UpdateRoleRequest.newBuilder());
            if (req == null) return;
            routeHelper.route(ctx, 200, update,
                req.toBuilder().setId(ctx.pathParam("id")).build());
        });

        r.delete("/roles/:id").handler(ctx ->
            routeHelper.routeNoContent(ctx, delete,
                DeleteByIdRequest.newBuilder().setId(ctx.pathParam("id")).build()));
    }
}
