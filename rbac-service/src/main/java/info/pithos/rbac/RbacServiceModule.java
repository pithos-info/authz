package info.pithos.rbac;

import info.pithos.rbac.impl.RelationalApiKeyService;
import info.pithos.rbac.impl.RelationalEnterpriseService;
import info.pithos.rbac.impl.RelationalGroupService;
import info.pithos.rbac.impl.RelationalRbacService;
import info.pithos.rbac.impl.RelationalRoleService;
import info.pithos.rbac.impl.RelationalUserService;
import info.pithos.runtime.core.context.ApplicationContext;
import info.pithos.runtime.core.context.ServiceModule;

public abstract class RbacServiceModule extends ServiceModule {

    protected RelationalRbacService service;
    protected RelationalEnterpriseService enterpriseService;
    protected RelationalUserService userService;
    protected RelationalGroupService groupService;
    protected RelationalRoleService roleService;
    protected RelationalApiKeyService apiKeyService;

    protected RbacServiceModule(ApplicationContext context) {
        super(context);
    }

    @Override
    protected void configure() {
        super.configure();
        super.bind(RbacService.class).toInstance(this.service);
        super.bind(RelationalRbacService.class).toInstance(this.service);
        super.bind(EnterpriseService.class).toInstance(this.enterpriseService);
        super.bind(RelationalEnterpriseService.class).toInstance(this.enterpriseService);
        super.bind(UserService.class).toInstance(this.userService);
        super.bind(RelationalUserService.class).toInstance(this.userService);
        super.bind(GroupService.class).toInstance(this.groupService);
        super.bind(RelationalGroupService.class).toInstance(this.groupService);
        super.bind(RoleService.class).toInstance(this.roleService);
        super.bind(RelationalRoleService.class).toInstance(this.roleService);
        super.bind(ApiKeyService.class).toInstance(this.apiKeyService);
        super.bind(RelationalApiKeyService.class).toInstance(this.apiKeyService);
    }
}
