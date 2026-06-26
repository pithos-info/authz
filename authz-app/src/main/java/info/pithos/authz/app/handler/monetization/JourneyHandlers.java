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
import info.pithos.monetization.service.CreateJourneyRequest;

import info.pithos.monetization.service.GetByIdRequest;
import info.pithos.monetization.service.Journey;
import info.pithos.monetization.service.JourneyList;
import info.pithos.monetization.service.JourneyService;
import info.pithos.runtime.core.context.ErrorCode;
import info.pithos.runtime.core.context.ServiceException;
import info.pithos.runtime.model.protocol.Context.RequestContext;
import info.pithos.serde.ProtoBufMapper;
import info.pithos.service.container.core.BaseServiceHandler;
import io.smallrye.mutiny.Uni;

public final class JourneyHandlers {

    private JourneyHandlers() {}

    public static final class Create extends BaseServiceHandler<CreateJourneyRequest, Journey> {
        private final JourneyService service;

        @Inject
        public Create(OAuthClient oAuthClient, JourneyService service) {
            super(oAuthClient);
            this.service = service;
        }

        @Override
        public Uni<Journey> handle(CreateJourneyRequest req, RequestContext rc) {
            Monetization.Journey data = Monetization.Journey.newBuilder()
                .setAppId(req.getAppId())
                .setName(req.getName())
                .setDisplayName(req.getDisplayName())
                .setDescription(req.getDescription())
                .setGoal(req.getGoal())
                .setOutcomeStatement(req.getOutcomeStatement())
                .setVersion(1)
                .build();
            return Uni.createFrom().completionStage(() -> service.create(rc, data))
                .map(created -> ProtoBufMapper.map(created, Journey.newBuilder()));
        }
    }

    public static final class Get extends BaseServiceHandler<GetByIdRequest, Journey> {
        private final JourneyService service;

        @Inject
        public Get(OAuthClient oAuthClient, JourneyService service) {
            super(oAuthClient);
            this.service = service;
        }

        @Override
        public Uni<Journey> handle(GetByIdRequest req, RequestContext rc) {
            return Uni.createFrom().completionStage(() -> service.get(rc, req.getId()))
                .map(opt -> opt.map(d -> ProtoBufMapper.<Journey>map(d, Journey.newBuilder()))
                    .orElseThrow(() -> new ServiceException(ErrorCode.NOT_FOUND,
                        "Journey not found: " + req.getId())));
        }
    }

    public static final class ListByApp extends BaseServiceHandler<GetByIdRequest, JourneyList> {
        private final JourneyService service;

        @Inject
        public ListByApp(OAuthClient oAuthClient, JourneyService service) {
            super(oAuthClient);
            this.service = service;
        }

        @Override
        public Uni<JourneyList> handle(GetByIdRequest req, RequestContext rc) {
            return Uni.createFrom().completionStage(() -> service.listByApp(rc, req.getId()))
                .map(items -> JourneyList.newBuilder()
                    .addAllJourneys(items.stream()
                        .map(d -> ProtoBufMapper.<Journey>map(d, Journey.newBuilder()))
                        .toList())
                    .build());
        }
    }
}
