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

import info.pithos.data.relational.client.RelationalClient;
import info.pithos.data.relational.client.ProtoBufAssociationService;
import info.pithos.rbac.UserRoleService;
import info.pithos.rbac.model.Rbac;
import info.pithos.runtime.model.protocol.Context.RequestContext;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class RelationalUserRoleService extends ProtoBufAssociationService<Rbac.UserRole>
        implements UserRoleService {

    public RelationalUserRoleService(RelationalClient relationalClient) {
        super(relationalClient, "userRole", Rbac.UserRole.getDefaultInstance(), "userId", "roleId");
    }

    @Override
    public CompletableFuture<Rbac.UserRole> grant(RequestContext rc, String userId, String roleId) {
        Rbac.UserRole userRole = Rbac.UserRole.newBuilder()
            .setEnterpriseId(authEnterpriseId(rc))
            .setUserId(userId)
            .setRoleId(roleId)
            .setGrantedById(authUserId(rc))
            .build();
        return insert(rc, userRole);
    }

    @Override
    public CompletableFuture<Void> revoke(RequestContext rc, String userId, String roleId) {
        Rbac.UserRole key = Rbac.UserRole.newBuilder()
            .setUserId(userId).setRoleId(roleId).build();
        return deleteByKey(rc, key);
    }

    @Override
    public CompletableFuture<Optional<Rbac.UserRole>> get(RequestContext rc, String userId, String roleId) {
        Rbac.UserRole key = Rbac.UserRole.newBuilder()
            .setUserId(userId).setRoleId(roleId).build();
        return getByKey(rc, key);
    }

    @Override
    public CompletableFuture<Boolean> hasRole(RequestContext rc, String roleId) {
        String uid = authUserId(rc);
        return relationalClient.query(dc(rc),
            """
            SELECT (
                EXISTS (
                    SELECT 1 FROM "userRole" WHERE "userId" = ? AND "roleId" = ?
                    UNION ALL
                    SELECT 1 FROM "groupRole" gr
                    JOIN "groupMember" gm ON gm."groupId" = gr."groupId"
                    WHERE gm."userId" = ? AND gr."roleId" = ?
                )
                AND EXISTS (SELECT 1 FROM "user" WHERE id = ? AND "enterpriseId" = ? AND deleted = false)
            ) AS result
            """, uid, roleId, uid, roleId, uid, authEnterpriseId(rc))
            .thenApply(rows -> rows.get(0).getBoolean("result"));
    }
}
