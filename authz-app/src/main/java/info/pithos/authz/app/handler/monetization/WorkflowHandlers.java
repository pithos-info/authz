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

package info.pithos.authz.app.handler.monetization;

import com.google.inject.Inject;
import info.pithos.authn.OAuthClient;
import info.pithos.monetization.model.Monetization;
import info.pithos.monetization.service.CreateWorkflowRequest;
import info.pithos.monetization.service.Feature;
import info.pithos.monetization.service.FeatureService;
import info.pithos.monetization.service.GetByIdRequest;
import info.pithos.monetization.service.Workflow;
import info.pithos.monetization.service.WorkflowDetail;
import info.pithos.monetization.service.WorkflowDetailList;
import info.pithos.monetization.service.WorkflowFeatureService;
import info.pithos.monetization.service.WorkflowService;
import info.pithos.monetization.service.WorkflowStep;
import info.pithos.runtime.core.context.ErrorCode;
import info.pithos.runtime.core.context.ServiceException;
import info.pithos.runtime.model.protocol.Context.RequestContext;
import info.pithos.serde.ProtoBufMapper;
import info.pithos.service.container.core.BaseServiceHandler;
import io.smallrye.mutiny.Uni;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public final class WorkflowHandlers {

    private WorkflowHandlers() {}

    // Fetches the ordered steps for a workflow and resolves each featureId to its full
    // Feature record (all lookups in parallel). Used by both Get and ListByJourney.
    private static CompletableFuture<WorkflowDetail> assemble(
            RequestContext rc,
            Monetization.Workflow workflow,
            WorkflowFeatureService workflowFeatureService,
            FeatureService featureService) {
        return workflowFeatureService.listByWorkflow(rc, workflow.getId())
            .thenCompose(wfList -> {
                List<CompletableFuture<Optional<Monetization.Feature>>> featureFutures = wfList.stream()
                    .map(wf -> featureService.get(rc, wf.getFeatureId()))
                    .toList();
                return CompletableFuture.allOf(featureFutures.toArray(new CompletableFuture[0]))
                    .thenApply(ignored -> {
                        WorkflowDetail.Builder detail = WorkflowDetail.newBuilder()
                            .setWorkflow(ProtoBufMapper.<Workflow>map(workflow, Workflow.newBuilder()));
                        for (int i = 0; i < wfList.size(); i++) {
                            Monetization.WorkflowFeature wf = wfList.get(i);
                            Monetization.Feature feature = featureFutures.get(i).join()
                                .orElseThrow(() -> new ServiceException(ErrorCode.NOT_FOUND,
                                    "Feature not found: " + wf.getFeatureId()));
                            detail.addSteps(WorkflowStep.newBuilder()
                                .setStepOrder(wf.getStepOrder())
                                .setFeature(ProtoBufMapper.<Feature>map(feature, Feature.newBuilder()))
                                .build());
                        }
                        return detail.build();
                    });
            });
    }

    public static final class Create extends BaseServiceHandler<CreateWorkflowRequest, Workflow> {
        private final WorkflowService service;

        @Inject
        public Create(OAuthClient oAuthClient, WorkflowService service) {
            super(oAuthClient);
            this.service = service;
        }

        @Override
        public Uni<Workflow> handle(CreateWorkflowRequest req, RequestContext rc) {
            Monetization.Workflow data = Monetization.Workflow.newBuilder()
                .setAppId(req.getAppId())
                .setJourneyId(req.getJourneyId())
                .setDepthLevel(req.getDepthLevel())
                .setVersion(1)
                .build();
            return Uni.createFrom().completionStage(() -> service.create(rc, data))
                .map(created -> ProtoBufMapper.map(created, Workflow.newBuilder()));
        }
    }

    public static final class Get extends BaseServiceHandler<GetByIdRequest, WorkflowDetail> {
        private final WorkflowService        service;
        private final WorkflowFeatureService workflowFeatureService;
        private final FeatureService         featureService;

        @Inject
        public Get(OAuthClient oAuthClient,
                   WorkflowService service,
                   WorkflowFeatureService workflowFeatureService,
                   FeatureService featureService) {
            super(oAuthClient);
            this.service               = service;
            this.workflowFeatureService = workflowFeatureService;
            this.featureService        = featureService;
        }

        @Override
        public Uni<WorkflowDetail> handle(GetByIdRequest req, RequestContext rc) {
            return Uni.createFrom().completionStage(() -> service.get(rc, req.getId()))
                .map(opt -> opt.orElseThrow(() ->
                    new ServiceException(ErrorCode.NOT_FOUND, "Workflow not found: " + req.getId())))
                .flatMap(workflow -> Uni.createFrom().completionStage(
                    () -> assemble(rc, workflow, workflowFeatureService, featureService)));
        }
    }

    public static final class ListByJourney extends BaseServiceHandler<GetByIdRequest, WorkflowDetailList> {
        private final WorkflowService        service;
        private final WorkflowFeatureService workflowFeatureService;
        private final FeatureService         featureService;

        @Inject
        public ListByJourney(OAuthClient oAuthClient,
                              WorkflowService service,
                              WorkflowFeatureService workflowFeatureService,
                              FeatureService featureService) {
            super(oAuthClient);
            this.service               = service;
            this.workflowFeatureService = workflowFeatureService;
            this.featureService        = featureService;
        }

        @Override
        public Uni<WorkflowDetailList> handle(GetByIdRequest req, RequestContext rc) {
            return Uni.createFrom().completionStage(() -> service.listByJourney(rc, req.getId()))
                .flatMap(workflows -> {
                    List<CompletableFuture<WorkflowDetail>> detailFutures = workflows.stream()
                        .map(w -> assemble(rc, w, workflowFeatureService, featureService))
                        .toList();
                    return Uni.createFrom().completionStage(() ->
                        CompletableFuture.allOf(detailFutures.toArray(new CompletableFuture[0]))
                            .thenApply(ignored -> WorkflowDetailList.newBuilder()
                                .addAllWorkflows(detailFutures.stream()
                                    .map(CompletableFuture::join)
                                    .toList())
                                .build()));
                });
        }
    }
}
