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
