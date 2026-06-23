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

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface UserRoleService extends AssociationService<Rbac.UserRole> {

    /** Grants a role to a user. The caller from authContext is recorded as grantedById. */
    CompletableFuture<Rbac.UserRole> grant(RequestContext rc, String userId, String roleId);

    /** Revokes a role from a user. */
    CompletableFuture<Void> revoke(RequestContext rc, String userId, String roleId);

    /** Returns the assignment if it exists. */
    CompletableFuture<Optional<Rbac.UserRole>> get(RequestContext rc, String userId, String roleId);

    /** Returns {@code true} if {@code authContext.userId} holds {@code roleId} directly or via group membership. */
    CompletableFuture<Boolean> hasRole(RequestContext rc, String roleId);
}
