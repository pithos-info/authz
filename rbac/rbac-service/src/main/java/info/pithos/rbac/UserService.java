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

import info.pithos.data.relational.client.CrudService;
import info.pithos.rbac.model.Rbac;
import info.pithos.runtime.model.protocol.Context.RequestContext;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface UserService extends CrudService<Rbac.User> {

    /** Lists non-deleted users scoped to {@code authContext.enterpriseId}. */
    CompletableFuture<List<Rbac.User>> list(RequestContext rc);

    /** Returns non-deleted users who are members of {@code groupId}, ordered by email. */
    CompletableFuture<List<Rbac.User>> getUsersInGroup(RequestContext rc, String groupId);

    /** Finds a non-deleted user by IdP subject within {@code authContext.enterpriseId}. */
    CompletableFuture<Optional<Rbac.User>> findByExternalId(RequestContext rc, String externalId);
}
