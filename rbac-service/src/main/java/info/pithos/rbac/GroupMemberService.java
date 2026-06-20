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

public interface GroupMemberService extends AssociationService<Rbac.GroupMember> {

    /** Adds a user to a group. Returns the created membership. */
    CompletableFuture<Rbac.GroupMember> add(RequestContext rc, String groupId, String userId);

    /** Removes a user from a group. */
    CompletableFuture<Void> remove(RequestContext rc, String groupId, String userId);

    /** Returns the membership if it exists. */
    CompletableFuture<Optional<Rbac.GroupMember>> get(RequestContext rc, String groupId, String userId);

    /** Returns {@code true} if {@code authContext.userId} is a member of {@code groupId} within {@code authContext.enterpriseId}. */
    CompletableFuture<Boolean> isUserInGroup(RequestContext rc, String groupId);
}
