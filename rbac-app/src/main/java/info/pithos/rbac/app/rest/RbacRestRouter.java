package info.pithos.rbac.app.rest;

import com.google.inject.Inject;
import info.pithos.rbac.app.rest.resource.ApiKeyResource;
import info.pithos.rbac.app.rest.resource.AuthResource;
import info.pithos.rbac.app.rest.resource.EnterpriseResource;
import info.pithos.rbac.app.rest.resource.GroupMemberResource;
import info.pithos.rbac.app.rest.resource.GroupResource;
import info.pithos.rbac.app.rest.resource.GroupRoleResource;
import info.pithos.rbac.app.rest.resource.MeResource;
import info.pithos.rbac.app.rest.resource.RolePermissionResource;
import info.pithos.rbac.app.rest.resource.RoleResource;
import info.pithos.rbac.app.rest.resource.UserResource;
import info.pithos.rbac.app.rest.resource.UserRoleResource;
import io.vertx.ext.web.Router;

public final class RbacRestRouter {

    private final AuthResource           auth;
    private final EnterpriseResource     enterprises;
    private final UserResource           users;
    private final GroupResource          groups;
    private final RoleResource           roles;
    private final ApiKeyResource         apiKeys;
    private final GroupMemberResource    groupMembers;
    private final UserRoleResource       userRoles;
    private final GroupRoleResource      groupRoles;
    private final RolePermissionResource rolePermissions;
    private final MeResource             me;

    @Inject
    public RbacRestRouter(
            AuthResource           auth,
            EnterpriseResource     enterprises,
            UserResource           users,
            GroupResource          groups,
            RoleResource           roles,
            ApiKeyResource         apiKeys,
            GroupMemberResource    groupMembers,
            UserRoleResource       userRoles,
            GroupRoleResource      groupRoles,
            RolePermissionResource rolePermissions,
            MeResource             me) {
        this.auth            = auth;
        this.enterprises     = enterprises;
        this.users           = users;
        this.groups          = groups;
        this.roles           = roles;
        this.apiKeys         = apiKeys;
        this.groupMembers    = groupMembers;
        this.userRoles       = userRoles;
        this.groupRoles      = groupRoles;
        this.rolePermissions = rolePermissions;
        this.me              = me;
    }

    public void mount(Router router) {
        auth.mount(router);
        enterprises.mount(router);
        users.mount(router);
        groups.mount(router);
        roles.mount(router);
        apiKeys.mount(router);
        groupMembers.mount(router);
        userRoles.mount(router);
        groupRoles.mount(router);
        rolePermissions.mount(router);
        me.mount(router);
    }
}
