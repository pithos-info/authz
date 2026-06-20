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

import info.pithos.data.cache.DistributedCacheClient;
import info.pithos.data.relational.FilterCriteria;
import info.pithos.data.relational.PreparedQuery;
import info.pithos.rbac.GroupService;
import info.pithos.data.relational.client.RelationalClient;
import info.pithos.data.relational.client.ProtoBufCrudService;
import info.pithos.rbac.model.Rbac;
import info.pithos.runtime.core.context.AsyncTaskQueue;
import info.pithos.runtime.model.protocol.Context.RequestContext;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class RelationalGroupService extends ProtoBufCrudService<Rbac.Group> implements GroupService {

    public RelationalGroupService(RelationalClient relationalClient,
                                   DistributedCacheClient cacheClient,
                                   AsyncTaskQueue taskQueue) {
        super(relationalClient, cacheClient, taskQueue, Rbac.Group.getDefaultInstance(), "deleted");
    }

    @Override
    public CompletableFuture<List<Rbac.Group>> list(RequestContext rc) {
        return cachedList(rc, FilterCriteria.eq("enterpriseId", authEnterpriseId(rc)).orderBy("name"));
    }

    @Override
    public CompletableFuture<List<Rbac.Group>> getUserGroups(RequestContext rc) {
        String sql = "SELECT " + store.statement().columnList()
            + " FROM \"group\""
            + " WHERE id IN (SELECT \"groupId\" FROM \"groupMember\" WHERE \"userId\" = ?)"
            + " AND \"enterpriseId\" = ? AND deleted = false"
            + " ORDER BY name";
        return query(rc, new PreparedQuery(sql, new Object[]{authUserId(rc), authEnterpriseId(rc)}));
    }
}
