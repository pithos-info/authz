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

package info.pithos.authz.app.grpc.monetization;

import com.google.inject.Inject;
import info.pithos.monetization.service.CreateJourneyRequest;
import info.pithos.monetization.service.GetByIdRequest;
import info.pithos.monetization.service.Journey;
import info.pithos.monetization.service.JourneyList;
import info.pithos.monetization.service.JourneyServiceGrpc;
import info.pithos.authz.app.handler.monetization.JourneyHandlers;
import info.pithos.service.container.core.grpc.GrpcSupport;
import io.grpc.stub.StreamObserver;

public final class JourneyGrpcService extends JourneyServiceGrpc.JourneyServiceImplBase {

    private final JourneyHandlers.Create    create;
    private final JourneyHandlers.Get       get;
    private final JourneyHandlers.ListByApp listByApp;

    @Inject
    public JourneyGrpcService(
            JourneyHandlers.Create    create,
            JourneyHandlers.Get       get,
            JourneyHandlers.ListByApp listByApp) {
        this.create    = create;
        this.get       = get;
        this.listByApp = listByApp;
    }

    @Override public void create(CreateJourneyRequest req, StreamObserver<Journey> obs)     { GrpcSupport.respond(create.handle(req, GrpcSupport.context()), obs); }
    @Override public void get(GetByIdRequest req, StreamObserver<Journey> obs)              { GrpcSupport.respond(get.handle(req, GrpcSupport.context()), obs); }
    @Override public void listByApp(GetByIdRequest req, StreamObserver<JourneyList> obs)    { GrpcSupport.respond(listByApp.handle(req, GrpcSupport.context()), obs); }
}
