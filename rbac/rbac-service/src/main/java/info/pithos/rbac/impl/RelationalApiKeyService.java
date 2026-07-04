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

package info.pithos.rbac.impl;

import info.pithos.data.relational.FilterCriteria;
import info.pithos.data.relational.PreparedQuery;
import info.pithos.rbac.ApiKeyService;
import info.pithos.data.relational.client.RelationalClient;
import info.pithos.data.relational.client.ProtoBufCrudService;
import info.pithos.rbac.RbacOperation;
import info.pithos.rbac.model.Rbac;
import info.pithos.runtime.core.context.ApplicationContext;
import info.pithos.runtime.core.metrics.MetricsCommitter;
import info.pithos.runtime.model.protocol.Context.RequestContext;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class RelationalApiKeyService extends ProtoBufCrudService<Rbac.ApiKey> implements ApiKeyService {

    private final MetricsCommitter mc;

    public RelationalApiKeyService(ApplicationContext applicationContext, RelationalClient relationalClient) {
        super(relationalClient, "apiKey", Rbac.ApiKey.getDefaultInstance());
        this.mc = applicationContext.getMetricsCommitter();
    }

    @Override
    public CompletableFuture<Void> revoke(RequestContext rc, String id) {
        long startMs = System.currentTimeMillis();
        return erase(rc, id)
            .whenComplete((v, ex) -> RbacOperation.record(mc, rc, RbacOperation.APIKEY_REVOKE, startMs, ex));
    }

    @Override
    public CompletableFuture<List<Rbac.ApiKey>> list(RequestContext rc) {
        return query(rc, FilterCriteria.eq("enterpriseId", authEnterpriseId(rc))
                                      .and(FilterCriteria.eq("userId", authUserId(rc)))
                                      .orderBy("utcCreatedAt"));
    }

    @Override
    public CompletableFuture<Optional<Rbac.ApiKey>> findByKeyHash(RequestContext rc, String keyHash) {
        long startMs = System.currentTimeMillis();
        return query(rc, FilterCriteria.eq("keyHash", keyHash))
            .thenApply(list -> list.isEmpty() ? Optional.empty() : Optional.of(list.get(0)))
            .whenComplete((v, ex) -> RbacOperation.record(mc, rc, RbacOperation.APIKEY_RESOLVE, startMs, ex));
    }

    @Override
    public CompletableFuture<Void> touch(RequestContext rc, String id) {
        long startMs = System.currentTimeMillis();
        String sql = "UPDATE \"apiKey\" SET \"lastUsedAt\" = now() WHERE id = ?";
        return relationalClient.execute(dc(rc), new PreparedQuery(sql, new Object[]{id}))
            .thenAccept(n -> {})
            .whenComplete((v, ex) -> RbacOperation.record(mc, rc, RbacOperation.APIKEY_TOUCH, startMs, ex));
    }
}
