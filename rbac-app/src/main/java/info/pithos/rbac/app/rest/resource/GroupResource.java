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
import info.pithos.rbac.app.handler.GroupHandlers;
import info.pithos.rbac.service.CreateGroupRequest;
import info.pithos.rbac.service.DeleteByIdRequest;
import info.pithos.rbac.service.Empty;
import info.pithos.rbac.service.GetByIdRequest;
import info.pithos.rbac.service.UpdateGroupRequest;
import info.pithos.service.container.core.BaseServiceHandler;
import io.vertx.ext.web.Router;

public final class GroupResource {

    private final GroupHandlers.Create create;
    private final GroupHandlers.Get    get;
    private final GroupHandlers.Update update;
    private final GroupHandlers.Delete delete;
    private final GroupHandlers.List   list;

    @Inject
    public GroupResource(
            GroupHandlers.Create create,
            GroupHandlers.Get    get,
            GroupHandlers.Update update,
            GroupHandlers.Delete delete,
            GroupHandlers.List   list) {
        this.create = create;
        this.get    = get;
        this.update = update;
        this.delete = delete;
        this.list   = list;
    }

    public void mount(Router r) {
        r.get("/groups").handler(ctx ->
            BaseServiceHandler.route(ctx, 200, list, Empty.getDefaultInstance()));

        r.post("/groups").handler(ctx -> {
            CreateGroupRequest req = BaseServiceHandler.parseBody(ctx, CreateGroupRequest.newBuilder());
            if (req == null) return;
            BaseServiceHandler.route(ctx, 201, create, req);
        });

        r.get("/groups/:id").handler(ctx ->
            BaseServiceHandler.route(ctx, 200, get,
                GetByIdRequest.newBuilder().setId(ctx.pathParam("id")).build()));

        r.put("/groups/:id").handler(ctx -> {
            UpdateGroupRequest req = BaseServiceHandler.parseBody(ctx, UpdateGroupRequest.newBuilder());
            if (req == null) return;
            BaseServiceHandler.route(ctx, 200, update,
                req.toBuilder().setId(ctx.pathParam("id")).build());
        });

        r.delete("/groups/:id").handler(ctx ->
            BaseServiceHandler.routeNoContent(ctx, delete,
                DeleteByIdRequest.newBuilder().setId(ctx.pathParam("id")).build()));
    }
}
