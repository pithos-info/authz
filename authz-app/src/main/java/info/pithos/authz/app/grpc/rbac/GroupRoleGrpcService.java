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
import info.pithos.authz.app.handler.rbac.GroupRoleHandlers;
import info.pithos.rbac.service.AssignGroupRoleRequest;
import info.pithos.rbac.service.Empty;
import info.pithos.rbac.service.GetByIdRequest;
import info.pithos.rbac.service.GroupRole;
import info.pithos.rbac.service.GroupRoleList;
import info.pithos.rbac.service.GroupRoleServiceGrpc;
import info.pithos.rbac.service.UnassignGroupRoleRequest;
import info.pithos.service.container.core.grpc.GrpcSupport;
import io.grpc.stub.StreamObserver;

public final class GroupRoleGrpcService extends GroupRoleServiceGrpc.GroupRoleServiceImplBase {

    private final GroupRoleHandlers.Assign      assign;
    private final GroupRoleHandlers.Unassign    unassign;
    private final GroupRoleHandlers.ListByGroup listByGroup;

    @Inject
    public GroupRoleGrpcService(
            GroupRoleHandlers.Assign      assign,
            GroupRoleHandlers.Unassign    unassign,
            GroupRoleHandlers.ListByGroup listByGroup) {
        this.assign      = assign;
        this.unassign    = unassign;
        this.listByGroup = listByGroup;
    }

    @Override
    public void assign(AssignGroupRoleRequest request, StreamObserver<GroupRole> responseObserver) {
        GrpcSupport.respond(assign.handle(request, GrpcSupport.context()), responseObserver);
    }

    @Override
    public void unassign(UnassignGroupRoleRequest request, StreamObserver<Empty> responseObserver) {
        GrpcSupport.respond(unassign.handle(request, GrpcSupport.context()), responseObserver);
    }

    @Override
    public void listByGroup(GetByIdRequest request, StreamObserver<GroupRoleList> responseObserver) {
        GrpcSupport.respond(listByGroup.handle(request, GrpcSupport.context()), responseObserver);
    }
}
