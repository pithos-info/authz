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
import info.pithos.rbac.GroupService;
import info.pithos.rbac.RoleService;
import info.pithos.rbac.UserService;
import info.pithos.rbac.mcp.sdk.McpTool;
import info.pithos.rbac.model.Rbac;
import info.pithos.runtime.model.protocol.Context.RequestContext;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * MCP tools for reading the authenticated user's identity and group/role context.
 * Intended to be loaded by an agent at the start of a session to understand who
 * it is acting on behalf of.
 */
public class IdentityTools {

    private final UserService  userService;
    private final GroupService groupService;
    private final RoleService  roleService;

    @Inject
    public IdentityTools(UserService userService,
                         GroupService groupService,
                         RoleService roleService) {
        this.userService  = userService;
        this.groupService = groupService;
        this.roleService  = roleService;
    }

    @McpTool(
        name        = "get_current_user",
        description = "Returns the profile of the authenticated user."
    )
    public CompletableFuture<Optional<Rbac.User>> getCurrentUser(RequestContext rc) {
        return userService.get(rc, rc.getAuthContext().getUserId());
    }

    @McpTool(
        name        = "get_user_groups",
        description = "Returns all groups the authenticated user belongs to "
                    + "within their enterprise."
    )
    public CompletableFuture<List<Rbac.Group>> getUserGroups(RequestContext rc) {
        return groupService.getUserGroups(rc);
    }

    @McpTool(
        name        = "get_user_roles",
        description = "Returns all roles held by the authenticated user, "
                    + "including those inherited via group membership."
    )
    public CompletableFuture<List<Rbac.Role>> getUserRoles(RequestContext rc) {
        return roleService.getUserRoles(rc);
    }
}
