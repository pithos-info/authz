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

package info.pithos.authz.app.rest;

import com.google.inject.Inject;
import info.pithos.authz.app.rest.resource.rbac.ApiKeyResource;
import info.pithos.authz.app.rest.resource.monetization.AppResource;
import info.pithos.authz.app.rest.resource.rbac.AuthResource;
import info.pithos.authz.app.rest.resource.monetization.FeatureResource;
import info.pithos.authz.app.rest.resource.monetization.JourneyResource;
import info.pithos.authz.app.rest.resource.monetization.WorkflowFeatureResource;
import info.pithos.authz.app.rest.resource.monetization.WorkflowResource;
import info.pithos.authz.app.rest.resource.rbac.EnterpriseResource;
import info.pithos.authz.app.rest.resource.rbac.GroupMemberResource;
import info.pithos.authz.app.rest.resource.rbac.GroupResource;
import info.pithos.authz.app.rest.resource.rbac.GroupRoleResource;
import info.pithos.authz.app.rest.resource.rbac.MeResource;
import info.pithos.authz.app.rest.resource.rbac.RolePermissionResource;
import info.pithos.authz.app.rest.resource.rbac.RoleResource;
import info.pithos.authz.app.rest.resource.rbac.UserResource;
import info.pithos.authz.app.rest.resource.rbac.UserRoleResource;
import io.vertx.ext.web.Router;

public final class AuthZRestRouter {

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
    private final AppResource            apps;
    private final FeatureResource        features;
    private final JourneyResource        journeys;
    private final WorkflowResource       workflows;
    private final WorkflowFeatureResource workflowFeatures;

    @Inject
    public AuthZRestRouter(
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
            MeResource             me,
            AppResource            apps,
            FeatureResource        features,
            JourneyResource        journeys,
            WorkflowResource       workflows,
            WorkflowFeatureResource workflowFeatures) {
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
        this.apps            = apps;
        this.features        = features;
        this.journeys        = journeys;
        this.workflows       = workflows;
        this.workflowFeatures = workflowFeatures;
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
        apps.mount(router);
        features.mount(router);
        journeys.mount(router);
        workflows.mount(router);
        workflowFeatures.mount(router);
    }
}
