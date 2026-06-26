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
import info.pithos.monetization.service.CreateJourneyRequest;
import info.pithos.monetization.service.GetByIdRequest;
import info.pithos.authz.app.handler.monetization.JourneyHandlers;
import info.pithos.service.container.core.RouteHelper;
import io.vertx.ext.web.Router;

public final class JourneyResource {

    private final RouteHelper routeHelper;

    private final JourneyHandlers.Create    create;
    private final JourneyHandlers.Get       get;
    private final JourneyHandlers.ListByApp listByApp;

    @Inject
    public JourneyResource(RouteHelper routeHelper,
            JourneyHandlers.Create    create,
            JourneyHandlers.Get       get,
            JourneyHandlers.ListByApp listByApp) {
        this.routeHelper = routeHelper;
        this.create    = create;
        this.get       = get;
        this.listByApp = listByApp;
    }

    public void mount(Router r) {
        r.post("/journeys").handler(ctx -> {
            CreateJourneyRequest req = routeHelper.parseBody(ctx, CreateJourneyRequest.newBuilder());
            if (req == null) return;
            routeHelper.route(ctx, 201, create, req);
        });

        r.get("/journeys/:id").handler(ctx ->
            routeHelper.route(ctx, 200, get,
                GetByIdRequest.newBuilder().setId(ctx.pathParam("id")).build()));

        r.get("/apps/:appId/journeys").handler(ctx ->
            routeHelper.route(ctx, 200, listByApp,
                GetByIdRequest.newBuilder().setId(ctx.pathParam("appId")).build()));
    }
}
