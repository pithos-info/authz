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

package info.pithos.authz.app.handler.rbac;

import com.google.inject.Inject;
import info.pithos.authn.OAuthClient;
import info.pithos.rbac.ApiKeyService;
import info.pithos.rbac.model.Rbac;
import info.pithos.rbac.service.ApiKey;
import info.pithos.rbac.service.ApiKeyList;
import info.pithos.rbac.service.CreateApiKeyRequest;
import info.pithos.rbac.service.DeleteByIdRequest;
import info.pithos.rbac.service.Empty;
import info.pithos.rbac.service.GetByIdRequest;
import info.pithos.runtime.core.context.ErrorCode;
import info.pithos.runtime.core.context.ServiceException;
import info.pithos.runtime.model.protocol.Context.RequestContext;
import info.pithos.serde.ProtoBufMapper;
import info.pithos.service.container.core.BaseServiceHandler;
import io.smallrye.mutiny.Uni;

public final class ApiKeyHandlers {

    private ApiKeyHandlers() {}

    public static final class Create extends BaseServiceHandler<CreateApiKeyRequest, ApiKey> {
        private final ApiKeyService service;

        @Inject
        public Create(OAuthClient oAuthClient, ApiKeyService service) {
            super(oAuthClient);
            this.service = service;
        }

        @Override
        public Uni<ApiKey> handle(CreateApiKeyRequest req, RequestContext rc) {
            Rbac.ApiKey data = Rbac.ApiKey.newBuilder()
                .setUserId(req.getUserId())
                .setName(req.getName())
                .addAllPermissions(req.getPermissionsList())
                .setExpiresAt(req.getExpiresAt())
                .build();
            return Uni.createFrom().completionStage(() -> service.create(rc, data))
                .map(created -> ProtoBufMapper.map(created, ApiKey.newBuilder()));
        }
    }

    public static final class Get extends BaseServiceHandler<GetByIdRequest, ApiKey> {
        private final ApiKeyService service;

        @Inject
        public Get(OAuthClient oAuthClient, ApiKeyService service) {
            super(oAuthClient);
            this.service = service;
        }

        @Override
        public Uni<ApiKey> handle(GetByIdRequest req, RequestContext rc) {
            return Uni.createFrom().completionStage(() -> service.get(rc, req.getId()))
                .map(opt -> opt.orElseThrow(() ->
                    new ServiceException(ErrorCode.NOT_FOUND, "ApiKey not found: " + req.getId())))
                .map(data -> ProtoBufMapper.map(data, ApiKey.newBuilder()));
        }
    }

    public static final class Revoke extends BaseServiceHandler<DeleteByIdRequest, Empty> {
        private final ApiKeyService service;

        @Inject
        public Revoke(OAuthClient oAuthClient, ApiKeyService service) {
            super(oAuthClient);
            this.service = service;
        }

        @Override
        public Uni<Empty> handle(DeleteByIdRequest req, RequestContext rc) {
            return Uni.createFrom().completionStage(() -> service.revoke(rc, req.getId()))
                .map(v -> Empty.getDefaultInstance());
        }
    }

    public static final class List extends BaseServiceHandler<Empty, ApiKeyList> {
        private final ApiKeyService service;

        @Inject
        public List(OAuthClient oAuthClient, ApiKeyService service) {
            super(oAuthClient);
            this.service = service;
        }

        @Override
        public Uni<ApiKeyList> handle(Empty req, RequestContext rc) {
            return Uni.createFrom().completionStage(() -> service.list(rc))
                .map(items -> ApiKeyList.newBuilder()
                    .addAllApiKeys(items.stream()
                        .map(k -> ProtoBufMapper.<ApiKey>map(k, ApiKey.newBuilder()))
                        .toList())
                    .build());
        }
    }
}
