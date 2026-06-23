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
import info.pithos.monetization.service.CreateFeatureRequest;
import info.pithos.monetization.service.Feature;
import info.pithos.monetization.service.FeatureList;
import info.pithos.monetization.service.FeatureServiceGrpc;
import info.pithos.monetization.service.GetByIdRequest;
import info.pithos.authz.app.handler.monetization.FeatureHandlers;
import info.pithos.service.container.core.grpc.GrpcSupport;
import io.grpc.stub.StreamObserver;

public final class FeatureGrpcService extends FeatureServiceGrpc.FeatureServiceImplBase {

    private final FeatureHandlers.Create    create;
    private final FeatureHandlers.Get       get;
    private final FeatureHandlers.ListByApp listByApp;

    @Inject
    public FeatureGrpcService(
            FeatureHandlers.Create    create,
            FeatureHandlers.Get       get,
            FeatureHandlers.ListByApp listByApp) {
        this.create    = create;
        this.get       = get;
        this.listByApp = listByApp;
    }

    @Override public void create(CreateFeatureRequest req, StreamObserver<Feature> obs)     { GrpcSupport.respond(create.handle(req, GrpcSupport.context()), obs); }
    @Override public void get(GetByIdRequest req, StreamObserver<Feature> obs)              { GrpcSupport.respond(get.handle(req, GrpcSupport.context()), obs); }
    @Override public void listByApp(GetByIdRequest req, StreamObserver<FeatureList> obs)    { GrpcSupport.respond(listByApp.handle(req, GrpcSupport.context()), obs); }
}
