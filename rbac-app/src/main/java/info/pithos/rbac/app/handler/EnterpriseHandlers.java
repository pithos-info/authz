package info.pithos.rbac.app.handler;

import com.google.inject.Inject;
import info.pithos.auth.OAuthClient;
import info.pithos.rbac.EnterpriseService;
import info.pithos.rbac.GroupRoleService;
import info.pithos.rbac.GroupService;
import info.pithos.rbac.RolePermissionService;
import info.pithos.rbac.RoleService;
import info.pithos.rbac.model.Rbac;
import info.pithos.rbac.service.CreateEnterpriseRequest;
import info.pithos.rbac.service.DeleteByIdRequest;
import info.pithos.rbac.service.Empty;
import info.pithos.rbac.service.Enterprise;
import info.pithos.rbac.service.EnterpriseList;
import info.pithos.rbac.service.GetByIdRequest;
import info.pithos.rbac.service.UpdateEnterpriseRequest;
import info.pithos.runtime.core.context.ErrorCode;
import info.pithos.runtime.core.context.ServiceException;
import info.pithos.runtime.model.protocol.Context.RequestContext;
import info.pithos.serde.ProtoBufMapper;
import info.pithos.service.container.core.BaseServiceHandler;
import io.smallrye.mutiny.Uni;

public final class EnterpriseHandlers {

    private EnterpriseHandlers() {}

    public static final class Create extends BaseServiceHandler<CreateEnterpriseRequest, Enterprise> {
        private final EnterpriseService     service;
        private final GroupService          groupService;
        private final GroupRoleService      groupRoleService;
        private final RoleService           roleService;
        private final RolePermissionService rpService;

        @Inject
        public Create(OAuthClient oAuthClient, EnterpriseService service,
                      GroupService groupService, GroupRoleService groupRoleService,
                      RoleService roleService, RolePermissionService rpService) {
            super(oAuthClient);
            this.service          = service;
            this.groupService     = groupService;
            this.groupRoleService = groupRoleService;
            this.roleService      = roleService;
            this.rpService        = rpService;
        }

        @Override
        public Uni<Enterprise> handle(CreateEnterpriseRequest req, RequestContext rc) {
            Rbac.Enterprise data = Rbac.Enterprise.newBuilder()
                .setSlug(req.getSlug())
                .setName(req.getName())
                .setDomain(req.getDomain())
                .build();
            return Uni.createFrom().completionStage(() ->
                service.create(rc, data)
                    .thenCompose(created ->
                        RbacEnricher.apiEnterprise(rc, created, groupService, groupRoleService, roleService, rpService)));
        }
    }

    public static final class Get extends BaseServiceHandler<GetByIdRequest, Enterprise> {
        private final EnterpriseService     enterpriseService;
        private final GroupService          groupService;
        private final GroupRoleService      groupRoleService;
        private final RoleService           roleService;
        private final RolePermissionService rpService;

        @Inject
        public Get(OAuthClient oAuthClient, EnterpriseService enterpriseService,
                   GroupService groupService, GroupRoleService groupRoleService,
                   RoleService roleService, RolePermissionService rpService) {
            super(oAuthClient);
            this.enterpriseService = enterpriseService;
            this.groupService      = groupService;
            this.groupRoleService  = groupRoleService;
            this.roleService       = roleService;
            this.rpService         = rpService;
        }

        @Override
        public Uni<Enterprise> handle(GetByIdRequest req, RequestContext rc) {
            return Uni.createFrom().completionStage(() ->
                enterpriseService.get(rc, req.getId())
                    .thenApply(opt -> opt.orElseThrow(() ->
                        new ServiceException(ErrorCode.NOT_FOUND, "Enterprise not found: " + req.getId())))
                    .thenCompose(data ->
                        RbacEnricher.apiEnterprise(rc, data, groupService, groupRoleService, roleService, rpService)));
        }
    }

    public static final class Update extends BaseServiceHandler<UpdateEnterpriseRequest, Enterprise> {
        private final EnterpriseService     service;
        private final GroupService          groupService;
        private final GroupRoleService      groupRoleService;
        private final RoleService           roleService;
        private final RolePermissionService rpService;

        @Inject
        public Update(OAuthClient oAuthClient, EnterpriseService service,
                      GroupService groupService, GroupRoleService groupRoleService,
                      RoleService roleService, RolePermissionService rpService) {
            super(oAuthClient);
            this.service          = service;
            this.groupService     = groupService;
            this.groupRoleService = groupRoleService;
            this.roleService      = roleService;
            this.rpService        = rpService;
        }

        @Override
        public Uni<Enterprise> handle(UpdateEnterpriseRequest req, RequestContext rc) {
            Rbac.Enterprise data = Rbac.Enterprise.newBuilder()
                .setId(req.getId())
                .setName(req.getName())
                .setDomain(req.getDomain())
                .build();
            return Uni.createFrom().completionStage(() ->
                service.update(rc, data)
                    .thenCompose(updated ->
                        RbacEnricher.apiEnterprise(rc, updated, groupService, groupRoleService, roleService, rpService)));
        }
    }

    public static final class Delete extends BaseServiceHandler<DeleteByIdRequest, Empty> {
        private final EnterpriseService service;

        @Inject
        public Delete(OAuthClient oAuthClient, EnterpriseService service) {
            super(oAuthClient);
            this.service = service;
        }

        @Override
        public Uni<Empty> handle(DeleteByIdRequest req, RequestContext rc) {
            return Uni.createFrom().completionStage(() -> service.delete(rc, req.getId()))
                .map(v -> Empty.getDefaultInstance());
        }
    }

    public static final class List extends BaseServiceHandler<Empty, EnterpriseList> {
        private final EnterpriseService service;

        @Inject
        public List(OAuthClient oAuthClient, EnterpriseService service) {
            super(oAuthClient);
            this.service = service;
        }

        @Override
        public Uni<EnterpriseList> handle(Empty req, RequestContext rc) {
            return Uni.createFrom().completionStage(() -> service.list(rc))
                .map(items -> EnterpriseList.newBuilder()
                    .addAllEnterprises(items.stream()
                        .map(e -> ProtoBufMapper.<Enterprise>map(e, Enterprise.newBuilder()))
                        .toList())
                    .build());
        }
    }
}
