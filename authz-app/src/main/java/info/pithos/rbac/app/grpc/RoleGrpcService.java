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

package info.pithos.rbac.app.grpc;

import com.google.inject.Inject;
import info.pithos.rbac.app.handler.RoleHandlers;
import info.pithos.rbac.service.CreateRoleRequest;
import info.pithos.rbac.service.DeleteByIdRequest;
import info.pithos.rbac.service.Empty;
import info.pithos.rbac.service.GetByIdRequest;
import info.pithos.rbac.service.Role;
import info.pithos.rbac.service.RoleList;
import info.pithos.rbac.service.RoleServiceGrpc;
import info.pithos.rbac.service.UpdateRoleRequest;
import info.pithos.service.container.core.grpc.GrpcSupport;
import io.grpc.stub.StreamObserver;

public final class RoleGrpcService extends RoleServiceGrpc.RoleServiceImplBase {

    private final RoleHandlers.Create      create;
    private final RoleHandlers.Get         get;
    private final RoleHandlers.Update      update;
    private final RoleHandlers.Delete      delete;
    private final RoleHandlers.List        list;
    private final RoleHandlers.GetUserRoles getUserRoles;

    @Inject
    public RoleGrpcService(
            RoleHandlers.Create      create,
            RoleHandlers.Get         get,
            RoleHandlers.Update      update,
            RoleHandlers.Delete      delete,
            RoleHandlers.List        list,
            RoleHandlers.GetUserRoles getUserRoles) {
        this.create      = create;
        this.get         = get;
        this.update      = update;
        this.delete      = delete;
        this.list        = list;
        this.getUserRoles = getUserRoles;
    }

    @Override
    public void create(CreateRoleRequest request, StreamObserver<Role> responseObserver) {
        GrpcSupport.respond(create.handle(request, GrpcSupport.context()), responseObserver);
    }

    @Override
    public void get(GetByIdRequest request, StreamObserver<Role> responseObserver) {
        GrpcSupport.respond(get.handle(request, GrpcSupport.context()), responseObserver);
    }

    @Override
    public void update(UpdateRoleRequest request, StreamObserver<Role> responseObserver) {
        GrpcSupport.respond(update.handle(request, GrpcSupport.context()), responseObserver);
    }

    @Override
    public void delete(DeleteByIdRequest request, StreamObserver<Empty> responseObserver) {
        GrpcSupport.respond(delete.handle(request, GrpcSupport.context()), responseObserver);
    }

    @Override
    public void list(Empty request, StreamObserver<RoleList> responseObserver) {
        GrpcSupport.respond(list.handle(request, GrpcSupport.context()), responseObserver);
    }

    @Override
    public void getUserRoles(Empty request, StreamObserver<RoleList> responseObserver) {
        GrpcSupport.respond(getUserRoles.handle(request, GrpcSupport.context()), responseObserver);
    }
}
