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
import info.pithos.rbac.app.handler.UserHandlers;
import info.pithos.rbac.service.CreateUserRequest;
import info.pithos.rbac.service.DeleteByIdRequest;
import info.pithos.rbac.service.Empty;
import info.pithos.rbac.service.GetByIdRequest;
import info.pithos.rbac.service.UpdateUserRequest;
import info.pithos.rbac.service.User;
import info.pithos.rbac.service.UserList;
import info.pithos.rbac.service.UserServiceGrpc;
import info.pithos.service.container.core.grpc.GrpcSupport;
import io.grpc.stub.StreamObserver;

public final class UserGrpcService extends UserServiceGrpc.UserServiceImplBase {

    private final UserHandlers.Create         create;
    private final UserHandlers.Get            get;
    private final UserHandlers.Update         update;
    private final UserHandlers.Delete         delete;
    private final UserHandlers.List           list;
    private final UserHandlers.GetUsersInGroup getUsersInGroup;

    @Inject
    public UserGrpcService(
            UserHandlers.Create         create,
            UserHandlers.Get            get,
            UserHandlers.Update         update,
            UserHandlers.Delete         delete,
            UserHandlers.List           list,
            UserHandlers.GetUsersInGroup getUsersInGroup) {
        this.create         = create;
        this.get            = get;
        this.update         = update;
        this.delete         = delete;
        this.list           = list;
        this.getUsersInGroup = getUsersInGroup;
    }

    @Override
    public void create(CreateUserRequest request, StreamObserver<User> responseObserver) {
        GrpcSupport.respond(create.handle(request, GrpcSupport.context()), responseObserver);
    }

    @Override
    public void get(GetByIdRequest request, StreamObserver<User> responseObserver) {
        GrpcSupport.respond(get.handle(request, GrpcSupport.context()), responseObserver);
    }

    @Override
    public void update(UpdateUserRequest request, StreamObserver<User> responseObserver) {
        GrpcSupport.respond(update.handle(request, GrpcSupport.context()), responseObserver);
    }

    @Override
    public void delete(DeleteByIdRequest request, StreamObserver<Empty> responseObserver) {
        GrpcSupport.respond(delete.handle(request, GrpcSupport.context()), responseObserver);
    }

    @Override
    public void list(Empty request, StreamObserver<UserList> responseObserver) {
        GrpcSupport.respond(list.handle(request, GrpcSupport.context()), responseObserver);
    }

    @Override
    public void getUsersInGroup(GetByIdRequest request, StreamObserver<UserList> responseObserver) {
        GrpcSupport.respond(getUsersInGroup.handle(request, GrpcSupport.context()), responseObserver);
    }
}
