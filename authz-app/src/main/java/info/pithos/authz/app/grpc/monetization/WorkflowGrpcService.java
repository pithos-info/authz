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

package info.pithos.authz.app.grpc.monetization;

import com.google.inject.Inject;
import info.pithos.monetization.service.CreateWorkflowRequest;
import info.pithos.monetization.service.GetByIdRequest;
import info.pithos.monetization.service.Workflow;
import info.pithos.monetization.service.WorkflowDetail;
import info.pithos.monetization.service.WorkflowDetailList;
import info.pithos.monetization.service.WorkflowServiceGrpc;
import info.pithos.authz.app.handler.monetization.WorkflowHandlers;
import info.pithos.service.container.core.grpc.GrpcSupport;
import io.grpc.stub.StreamObserver;

public final class WorkflowGrpcService extends WorkflowServiceGrpc.WorkflowServiceImplBase {

    private final WorkflowHandlers.Create        create;
    private final WorkflowHandlers.Get           get;
    private final WorkflowHandlers.ListByJourney listByJourney;

    @Inject
    public WorkflowGrpcService(
            WorkflowHandlers.Create        create,
            WorkflowHandlers.Get           get,
            WorkflowHandlers.ListByJourney listByJourney) {
        this.create        = create;
        this.get           = get;
        this.listByJourney = listByJourney;
    }

    @Override public void create(CreateWorkflowRequest req, StreamObserver<Workflow> obs)           { GrpcSupport.respond(create.handle(req, GrpcSupport.context()), obs); }
    @Override public void get(GetByIdRequest req, StreamObserver<WorkflowDetail> obs)               { GrpcSupport.respond(get.handle(req, GrpcSupport.context()), obs); }
    @Override public void listByJourney(GetByIdRequest req, StreamObserver<WorkflowDetailList> obs) { GrpcSupport.respond(listByJourney.handle(req, GrpcSupport.context()), obs); }
}
