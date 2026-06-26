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
import com.google.protobuf.Empty;
import info.pithos.authn.OAuthClient;
import info.pithos.runtime.core.context.ApplicationContext;
import info.pithos.monetization.service.AddWorkflowFeatureRequest;
import info.pithos.monetization.service.GetByIdRequest;
import info.pithos.monetization.service.RemoveWorkflowFeatureRequest;
import info.pithos.monetization.service.WorkflowFeature;
import info.pithos.monetization.service.WorkflowFeatureList;
import info.pithos.monetization.service.WorkflowFeatureService;
import info.pithos.runtime.model.protocol.Context.RequestContext;
import info.pithos.serde.ProtoBufMapper;
import info.pithos.service.container.core.BaseServiceHandler;
import io.smallrye.mutiny.Uni;

public final class WorkflowFeatureHandlers {

    private WorkflowFeatureHandlers() {}

    public static final class Add extends BaseServiceHandler<AddWorkflowFeatureRequest, WorkflowFeature> {
        private final WorkflowFeatureService service;

        @Inject
        public Add(ApplicationContext applicationContext, OAuthClient oAuthClient, WorkflowFeatureService service) {
            super(applicationContext, oAuthClient);
            this.service = service;
        }

        @Override
        public Uni<WorkflowFeature> handle(AddWorkflowFeatureRequest req, RequestContext rc) {
            return Uni.createFrom()
                .completionStage(() -> service.add(rc, req.getWorkflowId(), req.getFeatureId(), req.getStepOrder()))
                .map(created -> ProtoBufMapper.map(created, WorkflowFeature.newBuilder()));
        }
    }

    public static final class Remove extends BaseServiceHandler<RemoveWorkflowFeatureRequest, Empty> {
        private final WorkflowFeatureService service;

        @Inject
        public Remove(ApplicationContext applicationContext, OAuthClient oAuthClient, WorkflowFeatureService service) {
            super(applicationContext, oAuthClient);
            this.service = service;
        }

        @Override
        public Uni<Empty> handle(RemoveWorkflowFeatureRequest req, RequestContext rc) {
            return Uni.createFrom()
                .completionStage(() -> service.remove(rc, req.getWorkflowId(), req.getFeatureId()))
                .map(v -> Empty.getDefaultInstance());
        }
    }

    public static final class ListByWorkflow extends BaseServiceHandler<GetByIdRequest, WorkflowFeatureList> {
        private final WorkflowFeatureService service;

        @Inject
        public ListByWorkflow(ApplicationContext applicationContext, OAuthClient oAuthClient, WorkflowFeatureService service) {
            super(applicationContext, oAuthClient);
            this.service = service;
        }

        @Override
        public Uni<WorkflowFeatureList> handle(GetByIdRequest req, RequestContext rc) {
            return Uni.createFrom()
                .completionStage(() -> service.listByWorkflow(rc, req.getId()))
                .map(items -> WorkflowFeatureList.newBuilder()
                    .addAllWorkflowFeatures(items.stream()
                        .map(d -> ProtoBufMapper.<WorkflowFeature>map(d, WorkflowFeature.newBuilder()))
                        .toList())
                    .build());
        }
    }
}
