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

package info.pithos.authz.app.rest.resource.rbac;

import com.google.inject.Inject;
import info.pithos.authz.app.handler.rbac.GroupHandlers;
import info.pithos.authz.app.handler.rbac.RoleHandlers;
import info.pithos.authz.app.handler.rbac.RolePermissionHandlers;
import info.pithos.authz.app.handler.rbac.UserRoleHandlers;
import info.pithos.rbac.service.CheckPermissionRequest;
import info.pithos.rbac.service.Empty;
import info.pithos.rbac.service.GetByIdRequest;
import info.pithos.service.container.core.BaseServiceHandler;
import io.vertx.ext.web.Router;

/**
 * Routes whose scope is the authenticated caller — userId and enterpriseId
 * are resolved from RequestContext (X-User-Id header or token subject).
 */
public final class MeResource {

    private final GroupHandlers.GetUserGroups             getUserGroups;
    private final RoleHandlers.GetUserRoles               getUserRoles;
    private final UserRoleHandlers.HasRole                hasRole;
    private final RolePermissionHandlers.GetUserPermissions getUserPermissions;
    private final RolePermissionHandlers.HasPermission    hasPermission;

    @Inject
    public MeResource(
            GroupHandlers.GetUserGroups             getUserGroups,
            RoleHandlers.GetUserRoles               getUserRoles,
            UserRoleHandlers.HasRole                hasRole,
            RolePermissionHandlers.GetUserPermissions getUserPermissions,
            RolePermissionHandlers.HasPermission    hasPermission) {
        this.getUserGroups      = getUserGroups;
        this.getUserRoles       = getUserRoles;
        this.hasRole            = hasRole;
        this.getUserPermissions = getUserPermissions;
        this.hasPermission      = hasPermission;
    }

    public void mount(Router r) {
        r.get("/me/groups").handler(ctx ->
            BaseServiceHandler.route(ctx, 200, getUserGroups, Empty.getDefaultInstance()));

        r.get("/me/roles").handler(ctx ->
            BaseServiceHandler.route(ctx, 200, getUserRoles, Empty.getDefaultInstance()));

        r.get("/me/roles/check").handler(ctx -> {
            String roleId = ctx.queryParam("roleId").stream().findFirst().orElse("");
            BaseServiceHandler.route(ctx, 200, hasRole,
                GetByIdRequest.newBuilder().setId(roleId).build());
        });

        r.get("/me/permissions").handler(ctx ->
            BaseServiceHandler.route(ctx, 200, getUserPermissions, Empty.getDefaultInstance()));

        r.get("/me/permissions/check").handler(ctx -> {
            String permission = ctx.queryParam("permission").stream().findFirst().orElse("");
            BaseServiceHandler.route(ctx, 200, hasPermission,
                CheckPermissionRequest.newBuilder().setPermission(permission).build());
        });
    }
}
