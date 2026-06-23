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
import info.pithos.authz.app.handler.rbac.UserRoleHandlers;
import info.pithos.rbac.service.Bool;
import info.pithos.rbac.service.Empty;
import info.pithos.rbac.service.GetByIdRequest;
import info.pithos.rbac.service.GrantUserRoleRequest;
import info.pithos.rbac.service.RevokeUserRoleRequest;
import info.pithos.rbac.service.UserRole;
import info.pithos.rbac.service.UserRoleList;
import info.pithos.rbac.service.UserRoleServiceGrpc;
import info.pithos.service.container.core.grpc.GrpcSupport;
import io.grpc.stub.StreamObserver;

public final class UserRoleGrpcService extends UserRoleServiceGrpc.UserRoleServiceImplBase {

    private final UserRoleHandlers.Grant      grant;
    private final UserRoleHandlers.Revoke     revoke;
    private final UserRoleHandlers.ListByUser listByUser;
    private final UserRoleHandlers.HasRole    hasRole;

    @Inject
    public UserRoleGrpcService(
            UserRoleHandlers.Grant      grant,
            UserRoleHandlers.Revoke     revoke,
            UserRoleHandlers.ListByUser listByUser,
            UserRoleHandlers.HasRole    hasRole) {
        this.grant      = grant;
        this.revoke     = revoke;
        this.listByUser = listByUser;
        this.hasRole    = hasRole;
    }

    @Override
    public void grant(GrantUserRoleRequest request, StreamObserver<UserRole> responseObserver) {
        GrpcSupport.respond(grant.handle(request, GrpcSupport.context()), responseObserver);
    }

    @Override
    public void revoke(RevokeUserRoleRequest request, StreamObserver<Empty> responseObserver) {
        GrpcSupport.respond(revoke.handle(request, GrpcSupport.context()), responseObserver);
    }

    @Override
    public void listByUser(GetByIdRequest request, StreamObserver<UserRoleList> responseObserver) {
        GrpcSupport.respond(listByUser.handle(request, GrpcSupport.context()), responseObserver);
    }

    @Override
    public void hasRole(GetByIdRequest request, StreamObserver<Bool> responseObserver) {
        GrpcSupport.respond(hasRole.handle(request, GrpcSupport.context()), responseObserver);
    }
}
