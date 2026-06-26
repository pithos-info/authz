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
import com.google.protobuf.Empty;
import info.pithos.monetization.service.AddWorkflowFeatureRequest;
import info.pithos.monetization.service.GetByIdRequest;
import info.pithos.monetization.service.RemoveWorkflowFeatureRequest;
import info.pithos.monetization.service.WorkflowFeature;
import info.pithos.monetization.service.WorkflowFeatureList;
import info.pithos.monetization.service.WorkflowFeatureServiceGrpc;
import info.pithos.authz.app.handler.monetization.WorkflowFeatureHandlers;
import info.pithos.service.container.core.grpc.GrpcSupport;
import io.grpc.stub.StreamObserver;

public final class WorkflowFeatureGrpcService extends WorkflowFeatureServiceGrpc.WorkflowFeatureServiceImplBase {

    private final WorkflowFeatureHandlers.Add             add;
    private final WorkflowFeatureHandlers.Remove          remove;
    private final WorkflowFeatureHandlers.ListByWorkflow  listByWorkflow;

    @Inject
    public WorkflowFeatureGrpcService(
            WorkflowFeatureHandlers.Add            add,
            WorkflowFeatureHandlers.Remove         remove,
            WorkflowFeatureHandlers.ListByWorkflow listByWorkflow) {
        this.add            = add;
        this.remove         = remove;
        this.listByWorkflow = listByWorkflow;
    }

    @Override public void add(AddWorkflowFeatureRequest req, StreamObserver<WorkflowFeature> obs)     { GrpcSupport.respond(add.handle(req, GrpcSupport.context()), obs); }
    @Override public void remove(RemoveWorkflowFeatureRequest req, StreamObserver<Empty> obs)         { GrpcSupport.respond(remove.handle(req, GrpcSupport.context()), obs); }
    @Override public void listByWorkflow(GetByIdRequest req, StreamObserver<WorkflowFeatureList> obs) { GrpcSupport.respond(listByWorkflow.handle(req, GrpcSupport.context()), obs); }
}
