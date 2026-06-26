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
import info.pithos.authz.app.handler.rbac.EnterpriseHandlers;
import info.pithos.rbac.service.CreateEnterpriseRequest;
import info.pithos.rbac.service.DeleteByIdRequest;
import info.pithos.rbac.service.Empty;
import info.pithos.rbac.service.GetByIdRequest;
import info.pithos.rbac.service.UpdateEnterpriseRequest;
import info.pithos.service.container.core.RouteHelper;
import io.vertx.ext.web.Router;

public final class EnterpriseResource {

    private final RouteHelper routeHelper;

    private final EnterpriseHandlers.Create create;
    private final EnterpriseHandlers.Get    get;
    private final EnterpriseHandlers.Update update;
    private final EnterpriseHandlers.Delete delete;
    private final EnterpriseHandlers.List   list;

    @Inject
    public EnterpriseResource(RouteHelper routeHelper,
            EnterpriseHandlers.Create create,
            EnterpriseHandlers.Get    get,
            EnterpriseHandlers.Update update,
            EnterpriseHandlers.Delete delete,
            EnterpriseHandlers.List   list) {
        this.routeHelper = routeHelper;
        this.create = create;
        this.get    = get;
        this.update = update;
        this.delete = delete;
        this.list   = list;
    }

    public void mount(Router r) {
        r.get("/enterprises").handler(ctx ->
            routeHelper.route(ctx, 200, list, Empty.getDefaultInstance()));

        r.post("/enterprises").handler(ctx -> {
            CreateEnterpriseRequest req = routeHelper.parseBody(ctx, CreateEnterpriseRequest.newBuilder());
            if (req == null) return;
            routeHelper.route(ctx, 201, create, req);
        });

        r.get("/enterprises/:id").handler(ctx ->
            routeHelper.route(ctx, 200, get,
                GetByIdRequest.newBuilder().setId(ctx.pathParam("id")).build()));

        r.put("/enterprises/:id").handler(ctx -> {
            UpdateEnterpriseRequest req = routeHelper.parseBody(ctx, UpdateEnterpriseRequest.newBuilder());
            if (req == null) return;
            routeHelper.route(ctx, 200, update,
                req.toBuilder().setId(ctx.pathParam("id")).build());
        });

        r.delete("/enterprises/:id").handler(ctx ->
            routeHelper.routeNoContent(ctx, delete,
                DeleteByIdRequest.newBuilder().setId(ctx.pathParam("id")).build()));
    }
}
