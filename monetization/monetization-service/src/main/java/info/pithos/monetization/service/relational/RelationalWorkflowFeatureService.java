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
import info.pithos.data.cache.ProtoBufListCache;
import info.pithos.data.relational.FilterCriteria;
import info.pithos.data.relational.client.ProtoBufAssociationService;
import info.pithos.data.relational.client.RelationalClient;
import info.pithos.monetization.model.Monetization;
import info.pithos.monetization.service.WorkflowFeatureService;
import info.pithos.runtime.core.context.AsyncTaskQueue;
import info.pithos.runtime.model.protocol.Context.RequestContext;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class RelationalWorkflowFeatureService
        extends ProtoBufAssociationService<Monetization.WorkflowFeature>
        implements WorkflowFeatureService {

    private final ProtoBufListCache<Monetization.WorkflowFeature> listCache;
    private final AsyncTaskQueue taskQueue;

    public RelationalWorkflowFeatureService(RelationalClient relationalClient,
                                             DistributedCacheClient cacheClient,
                                             AsyncTaskQueue taskQueue) {
        super(relationalClient, "workflowFeature",
              Monetization.WorkflowFeature.getDefaultInstance(),
              "workflowId", "featureId");
        this.listCache = ProtoBufListCache.of(cacheClient, Monetization.WorkflowFeature.getDefaultInstance());
        this.taskQueue = taskQueue;
    }

    @Override
    public CompletableFuture<Monetization.WorkflowFeature> add(
            RequestContext rc, String workflowId, String featureId, int stepOrder) {
        Monetization.WorkflowFeature link = Monetization.WorkflowFeature.newBuilder()
            .setWorkflowId(workflowId)
            .setFeatureId(featureId)
            .setStepOrder(stepOrder)
            .build();
        return insert(rc, link)
            .thenCompose(saved -> invalidate(rc, workflowId).thenApply(v -> saved));
    }

    @Override
    public CompletableFuture<Void> remove(RequestContext rc, String workflowId, String featureId) {
        Monetization.WorkflowFeature key = Monetization.WorkflowFeature.newBuilder()
            .setWorkflowId(workflowId)
            .setFeatureId(featureId)
            .build();
        return deleteByKey(rc, key)
            .thenCompose(v -> invalidate(rc, workflowId));
    }

    @Override
    public CompletableFuture<List<Monetization.WorkflowFeature>> listByWorkflow(
            RequestContext rc, String workflowId) {
        if (isNoCache(rc)) return select(rc, FilterCriteria.eq("workflowId", workflowId).orderBy("stepOrder"));
        return listCache.get(rc, workflowId).thenCompose(cached -> {
            if (cached != null) return CompletableFuture.completedFuture(cached);
            return select(rc, FilterCriteria.eq("workflowId", workflowId).orderBy("stepOrder"))
                .thenApply(list -> {
                    taskQueue.enqueue(() -> listCache.set(rc, workflowId, list));
                    return list;
                });
        });
    }

    private CompletableFuture<Void> invalidate(RequestContext rc, String workflowId) {
        return listCache.delete(rc, workflowId).thenAccept(v -> {});
    }

    private static boolean isNoCache(RequestContext rc) {
        String cc = rc.getCacheControl();
        return cc != null && cc.toLowerCase().contains("no-cache");
    }
}
