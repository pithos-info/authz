package info.pithos.rbac.app.handler;

import com.google.inject.Inject;
import info.pithos.auth.OAuthClient;
import info.pithos.data.relational.FilterCriteria;
import info.pithos.rbac.RolePermissionService;
import info.pithos.rbac.service.AddRolePermissionRequest;
import info.pithos.rbac.service.Bool;
import info.pithos.rbac.service.CheckPermissionRequest;
import info.pithos.rbac.service.Empty;
import info.pithos.rbac.service.GetByIdRequest;
import info.pithos.rbac.service.PermissionList;
import info.pithos.rbac.service.RemoveRolePermissionRequest;
import info.pithos.rbac.service.RolePermission;
import info.pithos.rbac.service.RolePermissionList;
import info.pithos.runtime.model.protocol.Context.RequestContext;
import info.pithos.serde.ProtoBufMapper;
import info.pithos.service.container.core.BaseServiceHandler;
import io.smallrye.mutiny.Uni;


public final class RolePermissionHandlers {

    private RolePermissionHandlers() {}

    public static final class Add extends BaseServiceHandler<AddRolePermissionRequest, RolePermission> {
        private final RolePermissionService service;

        @Inject
        public Add(OAuthClient oAuthClient, RolePermissionService service) {
            super(oAuthClient);
            this.service = service;
        }

        @Override
        public Uni<RolePermission> handle(AddRolePermissionRequest req, RequestContext rc) {
            return Uni.createFrom()
                .completionStage(() -> service.add(rc, req.getRoleId(), req.getPermission()))
                .map(created -> ProtoBufMapper.map(created, RolePermission.newBuilder()));
        }
    }

    public static final class Remove extends BaseServiceHandler<RemoveRolePermissionRequest, Empty> {
        private final RolePermissionService service;

        @Inject
        public Remove(OAuthClient oAuthClient, RolePermissionService service) {
            super(oAuthClient);
            this.service = service;
        }

        @Override
        public Uni<Empty> handle(RemoveRolePermissionRequest req, RequestContext rc) {
            return Uni.createFrom()
                .completionStage(() -> service.remove(rc, req.getRoleId(), req.getPermission()))
                .map(v -> Empty.getDefaultInstance());
        }
    }

    public static final class ListByRole extends BaseServiceHandler<GetByIdRequest, RolePermissionList> {
        private final RolePermissionService service;

        @Inject
        public ListByRole(OAuthClient oAuthClient, RolePermissionService service) {
            super(oAuthClient);
            this.service = service;
        }

        @Override
        public Uni<RolePermissionList> handle(GetByIdRequest req, RequestContext rc) {
            return Uni.createFrom()
                .completionStage(() -> service.select(rc,
                    FilterCriteria.eq("roleId", req.getId())))
                .map(items -> RolePermissionList.newBuilder()
                    .addAllPermissions(items.stream()
                        .map(rp -> ProtoBufMapper.<RolePermission>map(rp, RolePermission.newBuilder()))
                        .toList())
                    .build());
        }
    }

    public static final class HasPermission extends BaseServiceHandler<CheckPermissionRequest, Bool> {
        private final RolePermissionService service;

        @Inject
        public HasPermission(OAuthClient oAuthClient, RolePermissionService service) {
            super(oAuthClient);
            this.service = service;
        }

        @Override
        public Uni<Bool> handle(CheckPermissionRequest req, RequestContext rc) {
            return Uni.createFrom()
                .completionStage(() -> service.hasPermission(rc, req.getPermission()))
                .map(result -> Bool.newBuilder().setValue(result).build());
        }
    }

    public static final class GetUserPermissions extends BaseServiceHandler<Empty, PermissionList> {
        private final RolePermissionService service;

        @Inject
        public GetUserPermissions(OAuthClient oAuthClient, RolePermissionService service) {
            super(oAuthClient);
            this.service = service;
        }

        @Override
        public Uni<PermissionList> handle(Empty req, RequestContext rc) {
            return Uni.createFrom()
                .completionStage(() -> service.getUserPermissions(rc))
                .map(perms -> PermissionList.newBuilder().addAllPermissions(perms).build());
        }
    }
}
