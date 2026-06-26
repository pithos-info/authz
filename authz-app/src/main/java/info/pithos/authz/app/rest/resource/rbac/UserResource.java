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
import info.pithos.authz.app.handler.rbac.UserHandlers;
import info.pithos.rbac.service.CreateUserRequest;
import info.pithos.rbac.service.DeleteByIdRequest;
import info.pithos.rbac.service.Empty;
import info.pithos.rbac.service.GetByIdRequest;
import info.pithos.rbac.service.UpdateUserRequest;
import info.pithos.service.container.core.RouteHelper;
import io.vertx.ext.web.Router;

public final class UserResource {

    private final RouteHelper routeHelper;

    private final UserHandlers.Create         create;
    private final UserHandlers.Get            get;
    private final UserHandlers.Update         update;
    private final UserHandlers.Delete         delete;
    private final UserHandlers.List           list;
    private final UserHandlers.GetUsersInGroup getUsersInGroup;

    @Inject
    public UserResource(RouteHelper routeHelper,
            UserHandlers.Create         create,
            UserHandlers.Get            get,
            UserHandlers.Update         update,
            UserHandlers.Delete         delete,
            UserHandlers.List           list,
            UserHandlers.GetUsersInGroup getUsersInGroup) {
        this.routeHelper = routeHelper;
        this.create          = create;
        this.get             = get;
        this.update          = update;
        this.delete          = delete;
        this.list            = list;
        this.getUsersInGroup = getUsersInGroup;
    }

    public void mount(Router r) {
        r.get("/users").handler(ctx ->
            routeHelper.route(ctx, 200, list, Empty.getDefaultInstance()));

        r.post("/users").handler(ctx -> {
            CreateUserRequest req = routeHelper.parseBody(ctx, CreateUserRequest.newBuilder());
            if (req == null) return;
            routeHelper.route(ctx, 201, create, req);
        });

        r.get("/users/:id").handler(ctx ->
            routeHelper.route(ctx, 200, get,
                GetByIdRequest.newBuilder().setId(ctx.pathParam("id")).build()));

        r.put("/users/:id").handler(ctx -> {
            UpdateUserRequest req = routeHelper.parseBody(ctx, UpdateUserRequest.newBuilder());
            if (req == null) return;
            routeHelper.route(ctx, 200, update,
                req.toBuilder().setId(ctx.pathParam("id")).build());
        });

        r.delete("/users/:id").handler(ctx ->
            routeHelper.routeNoContent(ctx, delete,
                DeleteByIdRequest.newBuilder().setId(ctx.pathParam("id")).build()));

        r.get("/groups/:id/users").handler(ctx ->
            routeHelper.route(ctx, 200, getUsersInGroup,
                GetByIdRequest.newBuilder().setId(ctx.pathParam("id")).build()));
    }
}
