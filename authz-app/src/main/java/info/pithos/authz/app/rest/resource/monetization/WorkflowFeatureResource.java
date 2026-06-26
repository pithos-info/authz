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
import info.pithos.monetization.service.AddWorkflowFeatureRequest;
import info.pithos.monetization.service.GetByIdRequest;
import info.pithos.monetization.service.RemoveWorkflowFeatureRequest;
import info.pithos.authz.app.handler.monetization.WorkflowFeatureHandlers;
import info.pithos.service.container.core.RouteHelper;
import io.vertx.ext.web.Router;

public final class WorkflowFeatureResource {

    private final RouteHelper routeHelper;

    private final WorkflowFeatureHandlers.Add            add;
    private final WorkflowFeatureHandlers.Remove         remove;
    private final WorkflowFeatureHandlers.ListByWorkflow listByWorkflow;

    @Inject
    public WorkflowFeatureResource(RouteHelper routeHelper,
            WorkflowFeatureHandlers.Add            add,
            WorkflowFeatureHandlers.Remove         remove,
            WorkflowFeatureHandlers.ListByWorkflow listByWorkflow) {
        this.routeHelper = routeHelper;
        this.add            = add;
        this.remove         = remove;
        this.listByWorkflow = listByWorkflow;
    }

    public void mount(Router r) {
        r.get("/workflows/:workflowId/features").handler(ctx ->
            routeHelper.route(ctx, 200, listByWorkflow,
                GetByIdRequest.newBuilder().setId(ctx.pathParam("workflowId")).build()));

        r.post("/workflows/:workflowId/features").handler(ctx -> {
            AddWorkflowFeatureRequest req = routeHelper.parseBody(ctx, AddWorkflowFeatureRequest.newBuilder());
            if (req == null) return;
            routeHelper.route(ctx, 201, add,
                req.toBuilder().setWorkflowId(ctx.pathParam("workflowId")).build());
        });

        r.delete("/workflows/:workflowId/features/:featureId").handler(ctx ->
            routeHelper.routeNoContent(ctx, remove,
                RemoveWorkflowFeatureRequest.newBuilder()
                    .setWorkflowId(ctx.pathParam("workflowId"))
                    .setFeatureId(ctx.pathParam("featureId"))
                    .build()));
    }
}
