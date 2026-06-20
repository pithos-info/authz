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
import info.pithos.data.relational.client.RelationalClient;
import info.pithos.data.relational.client.ProtoBufCrudService;
import info.pithos.rbac.UserService;
import info.pithos.rbac.model.Rbac;
import info.pithos.runtime.model.protocol.Context.RequestContext;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class RelationalUserService extends ProtoBufCrudService<Rbac.User> implements UserService {

    public RelationalUserService(RelationalClient relationalClient) {
        super(relationalClient, Rbac.User.getDefaultInstance(), "deleted");
    }

    @Override
    public CompletableFuture<List<Rbac.User>> list(RequestContext rc) {
        return query(rc, FilterCriteria.eq("enterpriseId", authEnterpriseId(rc)).orderBy("utcCreatedAt"));
    }

    @Override
    public CompletableFuture<List<Rbac.User>> getUsersInGroup(RequestContext rc, String groupId) {
        String sql = "SELECT " + store.statement().columnList()
            + " FROM \"user\""
            + " WHERE id IN (SELECT \"userId\" FROM \"groupMember\" WHERE \"groupId\" = ?)"
            + " AND deleted = false"
            + " ORDER BY email";
        return query(rc, new PreparedQuery(sql, new Object[]{groupId}));
    }

    @Override
    public CompletableFuture<Optional<Rbac.User>> findByExternalId(RequestContext rc, String externalId) {
        return query(rc, FilterCriteria.eq("enterpriseId", authEnterpriseId(rc))
                                      .and(FilterCriteria.eq("externalId", externalId)))
            .thenApply(users -> users.isEmpty() ? Optional.empty() : Optional.of(users.get(0)));
    }
}
