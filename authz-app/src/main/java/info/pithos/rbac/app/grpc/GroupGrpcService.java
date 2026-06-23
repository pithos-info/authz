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
import info.pithos.rbac.app.handler.GroupHandlers;
import info.pithos.rbac.service.CreateGroupRequest;
import info.pithos.rbac.service.DeleteByIdRequest;
import info.pithos.rbac.service.Empty;
import info.pithos.rbac.service.GetByIdRequest;
import info.pithos.rbac.service.Group;
import info.pithos.rbac.service.GroupList;
import info.pithos.rbac.service.GroupServiceGrpc;
import info.pithos.rbac.service.UpdateGroupRequest;
import info.pithos.service.container.core.grpc.GrpcSupport;
import io.grpc.stub.StreamObserver;

public final class GroupGrpcService extends GroupServiceGrpc.GroupServiceImplBase {

    private final GroupHandlers.Create       create;
    private final GroupHandlers.Get          get;
    private final GroupHandlers.Update       update;
    private final GroupHandlers.Delete       delete;
    private final GroupHandlers.List         list;
    private final GroupHandlers.GetUserGroups getUserGroups;

    @Inject
    public GroupGrpcService(
            GroupHandlers.Create       create,
            GroupHandlers.Get          get,
            GroupHandlers.Update       update,
            GroupHandlers.Delete       delete,
            GroupHandlers.List         list,
            GroupHandlers.GetUserGroups getUserGroups) {
        this.create       = create;
        this.get          = get;
        this.update       = update;
        this.delete       = delete;
        this.list         = list;
        this.getUserGroups = getUserGroups;
    }

    @Override
    public void create(CreateGroupRequest request, StreamObserver<Group> responseObserver) {
        GrpcSupport.respond(create.handle(request, GrpcSupport.context()), responseObserver);
    }

    @Override
    public void get(GetByIdRequest request, StreamObserver<Group> responseObserver) {
        GrpcSupport.respond(get.handle(request, GrpcSupport.context()), responseObserver);
    }

    @Override
    public void update(UpdateGroupRequest request, StreamObserver<Group> responseObserver) {
        GrpcSupport.respond(update.handle(request, GrpcSupport.context()), responseObserver);
    }

    @Override
    public void delete(DeleteByIdRequest request, StreamObserver<Empty> responseObserver) {
        GrpcSupport.respond(delete.handle(request, GrpcSupport.context()), responseObserver);
    }

    @Override
    public void list(Empty request, StreamObserver<GroupList> responseObserver) {
        GrpcSupport.respond(list.handle(request, GrpcSupport.context()), responseObserver);
    }

    @Override
    public void getUserGroups(Empty request, StreamObserver<GroupList> responseObserver) {
        GrpcSupport.respond(getUserGroups.handle(request, GrpcSupport.context()), responseObserver);
    }
}
