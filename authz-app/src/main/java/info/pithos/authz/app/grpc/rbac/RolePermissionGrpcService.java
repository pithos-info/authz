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
import info.pithos.authz.app.handler.rbac.RolePermissionHandlers;
import info.pithos.rbac.service.AddRolePermissionRequest;
import info.pithos.rbac.service.Bool;
import info.pithos.rbac.service.CheckPermissionRequest;
import info.pithos.rbac.service.Empty;
import info.pithos.rbac.service.GetByIdRequest;
import info.pithos.rbac.service.PermissionList;
import info.pithos.rbac.service.RemoveRolePermissionRequest;
import info.pithos.rbac.service.RolePermission;
import info.pithos.rbac.service.RolePermissionList;
import info.pithos.rbac.service.RolePermissionServiceGrpc;
import info.pithos.service.container.core.grpc.GrpcSupport;
import io.grpc.stub.StreamObserver;

public final class RolePermissionGrpcService extends RolePermissionServiceGrpc.RolePermissionServiceImplBase {

    private final RolePermissionHandlers.Add                add;
    private final RolePermissionHandlers.Remove             remove;
    private final RolePermissionHandlers.ListByRole         listByRole;
    private final RolePermissionHandlers.HasPermission      hasPermission;
    private final RolePermissionHandlers.GetUserPermissions getUserPermissions;

    @Inject
    public RolePermissionGrpcService(
            RolePermissionHandlers.Add                add,
            RolePermissionHandlers.Remove             remove,
            RolePermissionHandlers.ListByRole         listByRole,
            RolePermissionHandlers.HasPermission      hasPermission,
            RolePermissionHandlers.GetUserPermissions getUserPermissions) {
        this.add               = add;
        this.remove            = remove;
        this.listByRole        = listByRole;
        this.hasPermission     = hasPermission;
        this.getUserPermissions = getUserPermissions;
    }

    @Override
    public void add(AddRolePermissionRequest request, StreamObserver<RolePermission> responseObserver) {
        GrpcSupport.respond(add.handle(request, GrpcSupport.context()), responseObserver);
    }

    @Override
    public void remove(RemoveRolePermissionRequest request, StreamObserver<Empty> responseObserver) {
        GrpcSupport.respond(remove.handle(request, GrpcSupport.context()), responseObserver);
    }

    @Override
    public void listByRole(GetByIdRequest request, StreamObserver<RolePermissionList> responseObserver) {
        GrpcSupport.respond(listByRole.handle(request, GrpcSupport.context()), responseObserver);
    }

    @Override
    public void hasPermission(CheckPermissionRequest request, StreamObserver<Bool> responseObserver) {
        GrpcSupport.respond(hasPermission.handle(request, GrpcSupport.context()), responseObserver);
    }

    @Override
    public void getUserPermissions(Empty request, StreamObserver<PermissionList> responseObserver) {
        GrpcSupport.respond(getUserPermissions.handle(request, GrpcSupport.context()), responseObserver);
    }
}
