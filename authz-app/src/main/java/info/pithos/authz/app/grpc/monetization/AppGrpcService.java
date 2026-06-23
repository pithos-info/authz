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
import info.pithos.monetization.service.App;
import info.pithos.monetization.service.AppList;
import info.pithos.monetization.service.AppServiceGrpc;
import info.pithos.monetization.service.CreateAppRequest;
import info.pithos.monetization.service.Empty;
import info.pithos.monetization.service.GetByIdRequest;
import info.pithos.authz.app.handler.monetization.AppHandlers;
import info.pithos.service.container.core.grpc.GrpcSupport;
import io.grpc.stub.StreamObserver;

public final class AppGrpcService extends AppServiceGrpc.AppServiceImplBase {

    private final AppHandlers.Create create;
    private final AppHandlers.Get    get;
    private final AppHandlers.List   list;

    @Inject
    public AppGrpcService(
            AppHandlers.Create create,
            AppHandlers.Get    get,
            AppHandlers.List   list) {
        this.create = create;
        this.get    = get;
        this.list   = list;
    }

    @Override public void create(CreateAppRequest req, StreamObserver<App> obs)     { GrpcSupport.respond(create.handle(req, GrpcSupport.context()), obs); }
    @Override public void get(GetByIdRequest req, StreamObserver<App> obs)          { GrpcSupport.respond(get.handle(req, GrpcSupport.context()), obs); }
    @Override public void list(Empty req, StreamObserver<AppList> obs)              { GrpcSupport.respond(list.handle(req, GrpcSupport.context()), obs); }
}
