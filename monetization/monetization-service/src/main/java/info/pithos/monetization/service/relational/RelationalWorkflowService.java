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

import info.pithos.data.cache.DistributedCacheClient;
import info.pithos.data.relational.FilterCriteria;
import info.pithos.data.relational.client.ProtoBufImmutableService;
import info.pithos.data.relational.client.RelationalClient;
import info.pithos.monetization.model.Monetization;
import info.pithos.monetization.service.WorkflowService;
import info.pithos.runtime.core.context.AsyncTaskQueue;
import info.pithos.runtime.model.protocol.Context.RequestContext;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class RelationalWorkflowService extends ProtoBufImmutableService<Monetization.Workflow>
        implements WorkflowService {

    public RelationalWorkflowService(RelationalClient relationalClient,
                                      DistributedCacheClient cacheClient,
                                      AsyncTaskQueue taskQueue) {
        super(relationalClient, cacheClient, taskQueue, Monetization.Workflow.getDefaultInstance());
    }

    @Override
    public CompletableFuture<List<Monetization.Workflow>> listByJourney(RequestContext rc, String journeyId) {
        return cachedList(rc, FilterCriteria.eq("journeyId", journeyId), journeyId);
    }

    @Override
    protected CompletableFuture<Monetization.Workflow> save(RequestContext rc, Monetization.Workflow entity) {
        return super.save(rc, entity)
            .thenCompose(saved -> invalidateListCache(rc, entity.getJourneyId()).thenApply(v -> saved));
    }

    @Override
    protected Monetization.Workflow withParent(Monetization.Workflow parent, Monetization.Workflow entityBase) {
        return entityBase.toBuilder()
            .setVersion(parent.getVersion() + 1)
            .setParentWorkflowId(parent.getId())
            .build();
    }
}
