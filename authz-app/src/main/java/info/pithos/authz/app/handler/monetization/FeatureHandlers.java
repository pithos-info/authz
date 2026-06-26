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
import info.pithos.auth.OAuthClient;
import info.pithos.monetization.model.Monetization;
import info.pithos.monetization.service.CreateFeatureRequest;

import info.pithos.monetization.service.Feature;
import info.pithos.monetization.service.FeatureList;
import info.pithos.monetization.service.FeatureService;
import info.pithos.monetization.service.GetByIdRequest;
import info.pithos.runtime.core.context.ErrorCode;
import info.pithos.runtime.core.context.ServiceException;
import info.pithos.runtime.model.protocol.Context.RequestContext;
import info.pithos.serde.ProtoBufMapper;
import info.pithos.service.container.core.BaseServiceHandler;
import io.smallrye.mutiny.Uni;

public final class FeatureHandlers {

    private FeatureHandlers() {}

    public static final class Create extends BaseServiceHandler<CreateFeatureRequest, Feature> {
        private final FeatureService service;

        @Inject
        public Create(OAuthClient oAuthClient, FeatureService service) {
            super(oAuthClient);
            this.service = service;
        }

        @Override
        public Uni<Feature> handle(CreateFeatureRequest req, RequestContext rc) {
            Monetization.Feature data = Monetization.Feature.newBuilder()
                .setAppId(req.getAppId())
                .setName(req.getName())
                .setRichardsonMaturityLevel(req.getRichardsonMaturityLevel())
                .setVersion(1)
                .build();
            return Uni.createFrom().completionStage(() -> service.create(rc, data))
                .map(created -> ProtoBufMapper.map(created, Feature.newBuilder()));
        }
    }

    public static final class Get extends BaseServiceHandler<GetByIdRequest, Feature> {
        private final FeatureService service;

        @Inject
        public Get(OAuthClient oAuthClient, FeatureService service) {
            super(oAuthClient);
            this.service = service;
        }

        @Override
        public Uni<Feature> handle(GetByIdRequest req, RequestContext rc) {
            return Uni.createFrom().completionStage(() -> service.get(rc, req.getId()))
                .map(opt -> opt.map(d -> ProtoBufMapper.<Feature>map(d, Feature.newBuilder()))
                    .orElseThrow(() -> new ServiceException(ErrorCode.NOT_FOUND,
                        "Feature not found: " + req.getId())));
        }
    }

    public static final class ListByApp extends BaseServiceHandler<GetByIdRequest, FeatureList> {
        private final FeatureService service;

        @Inject
        public ListByApp(OAuthClient oAuthClient, FeatureService service) {
            super(oAuthClient);
            this.service = service;
        }

        @Override
        public Uni<FeatureList> handle(GetByIdRequest req, RequestContext rc) {
            return Uni.createFrom().completionStage(() -> service.listByApp(rc, req.getId()))
                .map(items -> FeatureList.newBuilder()
                    .addAllFeatures(items.stream()
                        .map(d -> ProtoBufMapper.<Feature>map(d, Feature.newBuilder()))
                        .toList())
                    .build());
        }
    }
}
