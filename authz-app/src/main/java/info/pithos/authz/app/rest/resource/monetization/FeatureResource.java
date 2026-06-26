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
import info.pithos.monetization.service.CreateFeatureRequest;
import info.pithos.monetization.service.GetByIdRequest;
import info.pithos.authz.app.handler.monetization.FeatureHandlers;
import info.pithos.service.container.core.RouteHelper;
import io.vertx.ext.web.Router;

public final class FeatureResource {

    private final RouteHelper routeHelper;

    private final FeatureHandlers.Create    create;
    private final FeatureHandlers.Get       get;
    private final FeatureHandlers.ListByApp listByApp;

    @Inject
    public FeatureResource(RouteHelper routeHelper,
            FeatureHandlers.Create    create,
            FeatureHandlers.Get       get,
            FeatureHandlers.ListByApp listByApp) {
        this.routeHelper = routeHelper;
        this.create    = create;
        this.get       = get;
        this.listByApp = listByApp;
    }

    public void mount(Router r) {
        r.post("/features").handler(ctx -> {
            CreateFeatureRequest req = routeHelper.parseBody(ctx, CreateFeatureRequest.newBuilder());
            if (req == null) return;
            routeHelper.route(ctx, 201, create, req);
        });

        r.get("/features/:id").handler(ctx ->
            routeHelper.route(ctx, 200, get,
                GetByIdRequest.newBuilder().setId(ctx.pathParam("id")).build()));

        r.get("/apps/:appId/features").handler(ctx ->
            routeHelper.route(ctx, 200, listByApp,
                GetByIdRequest.newBuilder().setId(ctx.pathParam("appId")).build()));
    }
}
