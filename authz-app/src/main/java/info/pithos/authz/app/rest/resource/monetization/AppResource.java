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

package info.pithos.authz.app.rest.resource.monetization;

import com.google.inject.Inject;
import info.pithos.monetization.service.CreateAppRequest;
import com.google.protobuf.Empty;
import info.pithos.monetization.service.GetByIdRequest;
import info.pithos.authz.app.handler.monetization.AppHandlers;
import info.pithos.service.container.core.RouteHelper;
import io.vertx.ext.web.Router;

public final class AppResource {

    private final RouteHelper routeHelper;

    private final AppHandlers.Create create;
    private final AppHandlers.Get    get;
    private final AppHandlers.List   list;

    @Inject
    public AppResource(RouteHelper routeHelper,
            AppHandlers.Create create,
            AppHandlers.Get    get,
            AppHandlers.List   list) {
        this.routeHelper = routeHelper;
        this.create = create;
        this.get    = get;
        this.list   = list;
    }

    public void mount(Router r) {
        r.get("/apps").handler(ctx ->
            routeHelper.route(ctx, 200, list, Empty.getDefaultInstance()));

        r.post("/apps").handler(ctx -> {
            CreateAppRequest req = routeHelper.parseBody(ctx, CreateAppRequest.newBuilder());
            if (req == null) return;
            routeHelper.route(ctx, 201, create, req);
        });

        r.get("/apps/:id").handler(ctx ->
            routeHelper.route(ctx, 200, get,
                GetByIdRequest.newBuilder().setId(ctx.pathParam("id")).build()));
    }
}
