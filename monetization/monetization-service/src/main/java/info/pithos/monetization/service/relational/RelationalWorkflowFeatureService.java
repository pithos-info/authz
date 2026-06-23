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

package info.pithos.monetization.service.relational;

import info.pithos.data.relational.FilterCriteria;
import info.pithos.data.relational.client.ProtoBufAssociationService;
import info.pithos.data.relational.client.RelationalClient;
import info.pithos.monetization.model.Monetization;
import info.pithos.monetization.service.WorkflowFeatureService;
import info.pithos.runtime.model.protocol.Context.RequestContext;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class RelationalWorkflowFeatureService
        extends ProtoBufAssociationService<Monetization.WorkflowFeature>
        implements WorkflowFeatureService {

    public RelationalWorkflowFeatureService(RelationalClient relationalClient) {
        super(relationalClient, "workflowFeature",
              Monetization.WorkflowFeature.getDefaultInstance(),
              "workflowId", "featureId");
    }

    @Override
    public CompletableFuture<Monetization.WorkflowFeature> add(
            RequestContext rc, String workflowId, String featureId) {
        Monetization.WorkflowFeature link = Monetization.WorkflowFeature.newBuilder()
            .setWorkflowId(workflowId)
            .setFeatureId(featureId)
            .build();
        return insert(rc, link);
    }

    @Override
    public CompletableFuture<Void> remove(
            RequestContext rc, String workflowId, String featureId) {
        Monetization.WorkflowFeature key = Monetization.WorkflowFeature.newBuilder()
            .setWorkflowId(workflowId)
            .setFeatureId(featureId)
            .build();
        return deleteByKey(rc, key);
    }

    @Override
    public CompletableFuture<List<Monetization.WorkflowFeature>> listByWorkflow(
            RequestContext rc, String workflowId) {
        return select(rc, FilterCriteria.eq("workflowId", workflowId));
    }
}
