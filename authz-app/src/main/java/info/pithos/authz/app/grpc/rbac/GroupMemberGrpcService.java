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
import info.pithos.authz.app.handler.rbac.GroupMemberHandlers;
import info.pithos.rbac.service.AddGroupMemberRequest;
import info.pithos.rbac.service.Bool;
import info.pithos.rbac.service.Empty;
import info.pithos.rbac.service.GetByIdRequest;
import info.pithos.rbac.service.GroupMember;
import info.pithos.rbac.service.GroupMemberList;
import info.pithos.rbac.service.GroupMemberServiceGrpc;
import info.pithos.rbac.service.RemoveGroupMemberRequest;
import info.pithos.service.container.core.grpc.GrpcSupport;
import io.grpc.stub.StreamObserver;

public final class GroupMemberGrpcService extends GroupMemberServiceGrpc.GroupMemberServiceImplBase {

    private final GroupMemberHandlers.Add         add;
    private final GroupMemberHandlers.Remove      remove;
    private final GroupMemberHandlers.ListByGroup listByGroup;
    private final GroupMemberHandlers.IsUserInGroup isUserInGroup;

    @Inject
    public GroupMemberGrpcService(
            GroupMemberHandlers.Add         add,
            GroupMemberHandlers.Remove      remove,
            GroupMemberHandlers.ListByGroup listByGroup,
            GroupMemberHandlers.IsUserInGroup isUserInGroup) {
        this.add          = add;
        this.remove       = remove;
        this.listByGroup  = listByGroup;
        this.isUserInGroup = isUserInGroup;
    }

    @Override
    public void add(AddGroupMemberRequest request, StreamObserver<GroupMember> responseObserver) {
        GrpcSupport.respond(add.handle(request, GrpcSupport.context()), responseObserver);
    }

    @Override
    public void remove(RemoveGroupMemberRequest request, StreamObserver<Empty> responseObserver) {
        GrpcSupport.respond(remove.handle(request, GrpcSupport.context()), responseObserver);
    }

    @Override
    public void listByGroup(GetByIdRequest request, StreamObserver<GroupMemberList> responseObserver) {
        GrpcSupport.respond(listByGroup.handle(request, GrpcSupport.context()), responseObserver);
    }

    @Override
    public void isUserInGroup(GetByIdRequest request, StreamObserver<Bool> responseObserver) {
        GrpcSupport.respond(isUserInGroup.handle(request, GrpcSupport.context()), responseObserver);
    }
}
