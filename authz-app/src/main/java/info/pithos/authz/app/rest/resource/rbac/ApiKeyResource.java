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
import info.pithos.authz.app.handler.rbac.ApiKeyHandlers;
import info.pithos.rbac.service.CreateApiKeyRequest;
import info.pithos.rbac.service.DeleteByIdRequest;
import info.pithos.rbac.service.Empty;
import info.pithos.rbac.service.GetByIdRequest;
import info.pithos.service.container.core.RouteHelper;
import io.vertx.ext.web.Router;

public final class ApiKeyResource {

    private final RouteHelper routeHelper;

    private final ApiKeyHandlers.Create create;
    private final ApiKeyHandlers.Get    get;
    private final ApiKeyHandlers.Revoke revoke;
    private final ApiKeyHandlers.List   list;

    @Inject
    public ApiKeyResource(RouteHelper routeHelper,
            ApiKeyHandlers.Create create,
            ApiKeyHandlers.Get    get,
            ApiKeyHandlers.Revoke revoke,
            ApiKeyHandlers.List   list) {
        this.routeHelper = routeHelper;
        this.create = create;
        this.get    = get;
        this.revoke = revoke;
        this.list   = list;
    }

    public void mount(Router r) {
        r.get("/apikeys").handler(ctx ->
            routeHelper.route(ctx, 200, list, Empty.getDefaultInstance()));

        r.post("/apikeys").handler(ctx -> {
            CreateApiKeyRequest req = routeHelper.parseBody(ctx, CreateApiKeyRequest.newBuilder());
            if (req == null) return;
            routeHelper.route(ctx, 201, create, req);
        });

        r.get("/apikeys/:id").handler(ctx ->
            routeHelper.route(ctx, 200, get,
                GetByIdRequest.newBuilder().setId(ctx.pathParam("id")).build()));

        r.delete("/apikeys/:id").handler(ctx ->
            routeHelper.routeNoContent(ctx, revoke,
                DeleteByIdRequest.newBuilder().setId(ctx.pathParam("id")).build()));
    }
}
