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

package info.pithos.authz.app.handler.monetization;

import com.google.inject.Inject;
import info.pithos.authn.OAuthClient;
import info.pithos.monetization.model.Monetization;
import info.pithos.monetization.service.App;
import info.pithos.monetization.service.AppList;
import info.pithos.monetization.service.AppService;
import info.pithos.monetization.service.CreateAppRequest;
import com.google.protobuf.Empty;
import info.pithos.monetization.service.GetByIdRequest;
import info.pithos.runtime.core.context.ErrorCode;
import info.pithos.runtime.core.context.ServiceException;
import info.pithos.runtime.model.protocol.Context.RequestContext;
import info.pithos.serde.ProtoBufMapper;
import info.pithos.service.container.core.BaseServiceHandler;
import io.smallrye.mutiny.Uni;

public final class AppHandlers {

    private AppHandlers() {}

    public static final class Create extends BaseServiceHandler<CreateAppRequest, App> {
        private final AppService service;

        @Inject
        public Create(OAuthClient oAuthClient, AppService service) {
            super(oAuthClient);
            this.service = service;
        }

        @Override
        public Uni<App> handle(CreateAppRequest req, RequestContext rc) {
            Monetization.App data = Monetization.App.newBuilder()
                .setSlug(req.getSlug())
                .setName(req.getName())
                .setOwnerId(rc.getAuthContext().getUserId())
                .setVersion(1)
                .build();
            return Uni.createFrom().completionStage(() -> service.create(rc, data))
                .map(created -> ProtoBufMapper.map(created, App.newBuilder()));
        }
    }

    public static final class Get extends BaseServiceHandler<GetByIdRequest, App> {
        private final AppService service;

        @Inject
        public Get(OAuthClient oAuthClient, AppService service) {
            super(oAuthClient);
            this.service = service;
        }

        @Override
        public Uni<App> handle(GetByIdRequest req, RequestContext rc) {
            return Uni.createFrom().completionStage(() -> service.get(rc, req.getId()))
                .map(opt -> opt.map(d -> ProtoBufMapper.<App>map(d, App.newBuilder()))
                    .orElseThrow(() -> new ServiceException(ErrorCode.NOT_FOUND,
                        "App not found: " + req.getId())));
        }
    }

    public static final class List extends BaseServiceHandler<Empty, AppList> {
        private final AppService service;

        @Inject
        public List(OAuthClient oAuthClient, AppService service) {
            super(oAuthClient);
            this.service = service;
        }

        @Override
        public Uni<AppList> handle(Empty req, RequestContext rc) {
            return Uni.createFrom().completionStage(() -> service.list(rc))
                .map(items -> AppList.newBuilder()
                    .addAllApps(items.stream()
                        .map(d -> ProtoBufMapper.<App>map(d, App.newBuilder()))
                        .toList())
                    .build());
        }
    }
}
