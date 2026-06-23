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

package info.pithos.authz.app.grpc.rbac;

import com.google.inject.Inject;
import info.pithos.authz.app.handler.rbac.EnterpriseHandlers;
import info.pithos.rbac.service.CreateEnterpriseRequest;
import info.pithos.rbac.service.DeleteByIdRequest;
import info.pithos.rbac.service.Empty;
import info.pithos.rbac.service.Enterprise;
import info.pithos.rbac.service.EnterpriseList;
import info.pithos.rbac.service.EnterpriseServiceGrpc;
import info.pithos.rbac.service.GetByIdRequest;
import info.pithos.rbac.service.UpdateEnterpriseRequest;
import info.pithos.service.container.core.grpc.GrpcSupport;
import io.grpc.stub.StreamObserver;

public final class EnterpriseGrpcService extends EnterpriseServiceGrpc.EnterpriseServiceImplBase {

    private final EnterpriseHandlers.Create create;
    private final EnterpriseHandlers.Get    get;
    private final EnterpriseHandlers.Update update;
    private final EnterpriseHandlers.Delete delete;
    private final EnterpriseHandlers.List   list;

    @Inject
    public EnterpriseGrpcService(
            EnterpriseHandlers.Create create,
            EnterpriseHandlers.Get    get,
            EnterpriseHandlers.Update update,
            EnterpriseHandlers.Delete delete,
            EnterpriseHandlers.List   list) {
        this.create = create;
        this.get    = get;
        this.update = update;
        this.delete = delete;
        this.list   = list;
    }

    @Override
    public void create(CreateEnterpriseRequest request, StreamObserver<Enterprise> responseObserver) {
        GrpcSupport.respond(create.handle(request, GrpcSupport.context()), responseObserver);
    }

    @Override
    public void get(GetByIdRequest request, StreamObserver<Enterprise> responseObserver) {
        GrpcSupport.respond(get.handle(request, GrpcSupport.context()), responseObserver);
    }

    @Override
    public void update(UpdateEnterpriseRequest request, StreamObserver<Enterprise> responseObserver) {
        GrpcSupport.respond(update.handle(request, GrpcSupport.context()), responseObserver);
    }

    @Override
    public void delete(DeleteByIdRequest request, StreamObserver<Empty> responseObserver) {
        GrpcSupport.respond(delete.handle(request, GrpcSupport.context()), responseObserver);
    }

    @Override
    public void list(Empty request, StreamObserver<EnterpriseList> responseObserver) {
        GrpcSupport.respond(list.handle(request, GrpcSupport.context()), responseObserver);
    }
}
