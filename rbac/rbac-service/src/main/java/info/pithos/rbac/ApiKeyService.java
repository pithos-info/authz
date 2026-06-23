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

import info.pithos.rbac.model.Rbac;
import info.pithos.runtime.model.protocol.Context.RequestContext;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface ApiKeyService {

    /** Creates an API key; {@code apiKey.enterpriseId} and {@code apiKey.userId} must be set. */
    CompletableFuture<Rbac.ApiKey> create(RequestContext rc, Rbac.ApiKey apiKey);

    CompletableFuture<Optional<Rbac.ApiKey>> get(RequestContext rc, String id);

    CompletableFuture<Void> revoke(RequestContext rc, String id);

    /** Lists API keys for {@code authContext.userId} within {@code authContext.enterpriseId}. */
    CompletableFuture<List<Rbac.ApiKey>> list(RequestContext rc);

    CompletableFuture<Optional<Rbac.ApiKey>> findByKeyHash(RequestContext rc, String keyHash);

    CompletableFuture<Void> touch(RequestContext rc, String id);
}
