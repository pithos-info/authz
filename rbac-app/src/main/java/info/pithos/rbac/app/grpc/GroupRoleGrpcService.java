package info.pithos.rbac.app.grpc;

import com.google.inject.Inject;
import info.pithos.rbac.app.handler.GroupRoleHandlers;
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
