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
import info.pithos.monetization.service.CreateWorkflowRequest;
import info.pithos.monetization.service.GetByIdRequest;
import info.pithos.authz.app.handler.monetization.WorkflowHandlers;
import info.pithos.service.container.core.BaseServiceHandler;
import io.vertx.ext.web.Router;

public final class WorkflowResource {

    private final WorkflowHandlers.Create        create;
    private final WorkflowHandlers.Get           get;
    private final WorkflowHandlers.ListByApp     listByApp;
    private final WorkflowHandlers.ListByJourney listByJourney;

    @Inject
    public WorkflowResource(
            WorkflowHandlers.Create        create,
            WorkflowHandlers.Get           get,
            WorkflowHandlers.ListByApp     listByApp,
            WorkflowHandlers.ListByJourney listByJourney) {
        this.create        = create;
        this.get           = get;
        this.listByApp     = listByApp;
        this.listByJourney = listByJourney;
    }

    public void mount(Router r) {
        r.post("/workflows").handler(ctx -> {
            CreateWorkflowRequest req = BaseServiceHandler.parseBody(ctx, CreateWorkflowRequest.newBuilder());
            if (req == null) return;
            BaseServiceHandler.route(ctx, 201, create, req);
        });

        r.get("/workflows/:id").handler(ctx ->
            BaseServiceHandler.route(ctx, 200, get,
                GetByIdRequest.newBuilder().setId(ctx.pathParam("id")).build()));

        r.get("/apps/:appId/workflows").handler(ctx ->
            BaseServiceHandler.route(ctx, 200, listByApp,
                GetByIdRequest.newBuilder().setId(ctx.pathParam("appId")).build()));

        r.get("/journeys/:journeyId/workflows").handler(ctx ->
            BaseServiceHandler.route(ctx, 200, listByJourney,
                GetByIdRequest.newBuilder().setId(ctx.pathParam("journeyId")).build()));
    }
}
