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

package info.pithos.rbac.mcp.tools;

import com.google.inject.Inject;
import info.pithos.rbac.RolePermissionService;
import info.pithos.rbac.UserRoleService;
import info.pithos.rbac.mcp.sdk.McpTool;
import info.pithos.runtime.model.protocol.Context.RequestContext;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * MCP tools for authorization checks. These are the primary read surface for agents
 * that need to understand what the current user is allowed to do before taking action.
 *
 * All tools receive the session's {@link RequestContext} (authenticated at connection time)
 * so they are automatically scoped to the authenticated user's enterprise.
 */
public class AuthzTools {

    private final RolePermissionService rolePermissionService;
    private final UserRoleService userRoleService;

    @Inject
    public AuthzTools(RolePermissionService rolePermissionService,
                      UserRoleService userRoleService) {
        this.rolePermissionService = rolePermissionService;
        this.userRoleService       = userRoleService;
    }

    @McpTool(
        name        = "check_permission",
        description = "Returns true if the authenticated user holds the given permission, "
                    + "either directly via a role or transitively via group membership."
    )
    public CompletableFuture<Boolean> checkPermission(RequestContext rc, String permission) {
        return rolePermissionService.hasPermission(rc, permission);
    }

    @McpTool(
        name        = "check_role",
        description = "Returns true if the authenticated user holds the given role, "
                    + "either directly or via group membership."
    )
    public CompletableFuture<Boolean> checkRole(RequestContext rc, String roleId) {
        return userRoleService.hasRole(rc, roleId);
    }

    @McpTool(
        name        = "get_permissions",
        description = "Returns all permissions effective for the authenticated user "
                    + "across all direct roles and group-inherited roles."
    )
    public CompletableFuture<List<String>> getPermissions(RequestContext rc) {
        return rolePermissionService.getUserPermissions(rc);
    }
}
