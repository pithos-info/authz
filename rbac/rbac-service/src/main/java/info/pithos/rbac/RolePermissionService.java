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

package info.pithos.rbac;

import info.pithos.data.relational.client.AssociationService;
import info.pithos.rbac.model.Rbac;
import info.pithos.runtime.model.protocol.Context.RequestContext;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface RolePermissionService extends AssociationService<Rbac.RolePermission> {

    /** Adds a permission to a role. */
    CompletableFuture<Rbac.RolePermission> add(RequestContext rc, String roleId, String permission);

    /** Removes a permission from a role. */
    CompletableFuture<Void> remove(RequestContext rc, String roleId, String permission);

    /** Returns the assignment if it exists. */
    CompletableFuture<Optional<Rbac.RolePermission>> get(RequestContext rc, String roleId, String permission);

    /** Returns {@code true} if {@code authContext.userId} holds {@code permission} directly or via groups. */
    CompletableFuture<Boolean> hasPermission(RequestContext rc, String permission);

    /** Returns all distinct permissions effective for {@code authContext.userId}. */
    CompletableFuture<List<String>> getUserPermissions(RequestContext rc);
}
