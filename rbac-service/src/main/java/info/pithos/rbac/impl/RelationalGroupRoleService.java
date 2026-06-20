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

import info.pithos.rbac.GroupRoleService;
import info.pithos.data.relational.client.RelationalClient;
import info.pithos.data.relational.client.ProtoBufAssociationService;
import info.pithos.rbac.model.Rbac;
import info.pithos.runtime.model.protocol.Context.RequestContext;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class RelationalGroupRoleService extends ProtoBufAssociationService<Rbac.GroupRole>
        implements GroupRoleService {

    public RelationalGroupRoleService(RelationalClient relationalClient) {
        super(relationalClient, "groupRole", Rbac.GroupRole.getDefaultInstance(), "groupId", "roleId");
    }

    @Override
    public CompletableFuture<Rbac.GroupRole> assign(RequestContext rc, String groupId, String roleId) {
        Rbac.GroupRole groupRole = Rbac.GroupRole.newBuilder()
            .setEnterpriseId(authEnterpriseId(rc))
            .setGroupId(groupId)
            .setRoleId(roleId)
            .build();
        return insert(rc, groupRole);
    }

    @Override
    public CompletableFuture<Void> unassign(RequestContext rc, String groupId, String roleId) {
        Rbac.GroupRole key = Rbac.GroupRole.newBuilder()
            .setGroupId(groupId).setRoleId(roleId).build();
        return deleteByKey(rc, key);
    }

    @Override
    public CompletableFuture<Optional<Rbac.GroupRole>> get(RequestContext rc, String groupId, String roleId) {
        Rbac.GroupRole key = Rbac.GroupRole.newBuilder()
            .setGroupId(groupId).setRoleId(roleId).build();
        return getByKey(rc, key);
    }
}
