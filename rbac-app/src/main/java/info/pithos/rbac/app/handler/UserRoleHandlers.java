package info.pithos.rbac.app.handler;

import com.google.inject.Inject;
import info.pithos.auth.OAuthClient;
import info.pithos.data.relational.FilterCriteria;
import info.pithos.rbac.UserRoleService;
import info.pithos.rbac.service.Bool;
import info.pithos.rbac.service.Empty;
import info.pithos.rbac.service.GetByIdRequest;
import info.pithos.rbac.service.GrantUserRoleRequest;
import info.pithos.rbac.service.RevokeUserRoleRequest;
import info.pithos.rbac.service.UserRole;
import info.pithos.rbac.service.UserRoleList;
import info.pithos.runtime.model.protocol.Context.RequestContext;
import info.pithos.serde.ProtoBufMapper;
import info.pithos.service.container.core.BaseServiceHandler;
import io.smallrye.mutiny.Uni;


public final class UserRoleHandlers {

    private UserRoleHandlers() {}

    public static final class Grant extends BaseServiceHandler<GrantUserRoleRequest, UserRole> {
        private final UserRoleService service;

        @Inject
        public Grant(OAuthClient oAuthClient, UserRoleService service) {
            super(oAuthClient);
            this.service = service;
        }

        @Override
        public Uni<UserRole> handle(GrantUserRoleRequest req, RequestContext rc) {
            return Uni.createFrom()
                .completionStage(() -> service.grant(rc, req.getUserId(), req.getRoleId()))
                .map(created -> ProtoBufMapper.map(created, UserRole.newBuilder()));
        }
    }

    public static final class Revoke extends BaseServiceHandler<RevokeUserRoleRequest, Empty> {
        private final UserRoleService service;

        @Inject
        public Revoke(OAuthClient oAuthClient, UserRoleService service) {
            super(oAuthClient);
            this.service = service;
        }

        @Override
        public Uni<Empty> handle(RevokeUserRoleRequest req, RequestContext rc) {
            return Uni.createFrom()
                .completionStage(() -> service.revoke(rc, req.getUserId(), req.getRoleId()))
                .map(v -> Empty.getDefaultInstance());
        }
    }

    public static final class ListByUser extends BaseServiceHandler<GetByIdRequest, UserRoleList> {
        private final UserRoleService service;

        @Inject
        public ListByUser(OAuthClient oAuthClient, UserRoleService service) {
            super(oAuthClient);
            this.service = service;
        }

        @Override
        public Uni<UserRoleList> handle(GetByIdRequest req, RequestContext rc) {
            return Uni.createFrom()
                .completionStage(() -> service.select(rc,
                    FilterCriteria.eq("userId", req.getId())))
                .map(items -> UserRoleList.newBuilder()
                    .addAllUserRoles(items.stream()
                        .map(ur -> ProtoBufMapper.<UserRole>map(ur, UserRole.newBuilder()))
                        .toList())
                    .build());
        }
    }

    public static final class HasRole extends BaseServiceHandler<GetByIdRequest, Bool> {
        private final UserRoleService service;

        @Inject
        public HasRole(OAuthClient oAuthClient, UserRoleService service) {
            super(oAuthClient);
            this.service = service;
        }

        @Override
        public Uni<Bool> handle(GetByIdRequest req, RequestContext rc) {
            return Uni.createFrom()
                .completionStage(() -> service.hasRole(rc, req.getId()))
                .map(result -> Bool.newBuilder().setValue(result).build());
        }
    }
}
