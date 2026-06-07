package info.pithos.rbac;

import info.pithos.rbac.impl.RelationalApiKeyService;
import info.pithos.rbac.impl.RelationalEnterpriseService;
import info.pithos.rbac.impl.RelationalGroupMemberService;
import info.pithos.rbac.impl.RelationalGroupRoleService;
import info.pithos.rbac.impl.RelationalGroupService;
import info.pithos.rbac.impl.RelationalRolePermissionService;
import info.pithos.rbac.impl.RelationalRoleService;
import info.pithos.rbac.impl.RelationalUserRoleService;
import info.pithos.rbac.impl.RelationalUserService;
import info.pithos.runtime.core.context.ApplicationContext;
import info.pithos.runtime.core.context.ServiceModule;

public abstract class RbacServiceModule extends ServiceModule {

    protected RelationalEnterpriseService enterpriseService;
    protected RelationalUserService userService;
    protected RelationalGroupService groupService;
    protected RelationalGroupMemberService groupMemberService;
    protected RelationalUserRoleService userRoleService;
    protected RelationalGroupRoleService groupRoleService;
    protected RelationalRolePermissionService rolePermissionService;
    protected RelationalRoleService roleService;
    protected RelationalApiKeyService apiKeyService;

    protected RbacServiceModule(ApplicationContext context) {
        super(context);
    }

    @Override
    protected void configure() {
        super.configure();
        super.bind(EnterpriseService.class).toInstance(this.enterpriseService);
        super.bind(RelationalEnterpriseService.class).toInstance(this.enterpriseService);
        super.bind(UserService.class).toInstance(this.userService);
        super.bind(RelationalUserService.class).toInstance(this.userService);
        super.bind(GroupService.class).toInstance(this.groupService);
        super.bind(RelationalGroupService.class).toInstance(this.groupService);
        super.bind(GroupMemberService.class).toInstance(this.groupMemberService);
        super.bind(RelationalGroupMemberService.class).toInstance(this.groupMemberService);
        super.bind(UserRoleService.class).toInstance(this.userRoleService);
        super.bind(RelationalUserRoleService.class).toInstance(this.userRoleService);
        super.bind(GroupRoleService.class).toInstance(this.groupRoleService);
        super.bind(RelationalGroupRoleService.class).toInstance(this.groupRoleService);
        super.bind(RolePermissionService.class).toInstance(this.rolePermissionService);
        super.bind(RelationalRolePermissionService.class).toInstance(this.rolePermissionService);
        super.bind(RoleService.class).toInstance(this.roleService);
        super.bind(RelationalRoleService.class).toInstance(this.roleService);
        super.bind(ApiKeyService.class).toInstance(this.apiKeyService);
        super.bind(RelationalApiKeyService.class).toInstance(this.apiKeyService);
    }
}
