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

package info.pithos.rbac.mcp.resources;

import com.google.inject.Inject;
import info.pithos.rbac.GroupService;
import info.pithos.rbac.RolePermissionService;
import info.pithos.rbac.RoleService;
import info.pithos.rbac.UserService;
import info.pithos.rbac.mcp.sdk.McpResource;
import info.pithos.rbac.model.Rbac;
import info.pithos.runtime.model.protocol.Context.RequestContext;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * MCP resource: {@code rbac://user/context}
 *
 * Returns the authenticated user's complete access context in one read:
 * profile, groups, roles, and effective permissions.
 *
 * Agents typically load this resource once at the start of a session so they
 * understand who they are acting on behalf of and what that user is allowed to do,
 * before deciding which tools to call.
 */
public class UserContextResource {

    private final UserService           userService;
    private final GroupService          groupService;
    private final RoleService           roleService;
    private final RolePermissionService rolePermissionService;

    @Inject
    public UserContextResource(UserService userService,
                                GroupService groupService,
                                RoleService roleService,
                                RolePermissionService rolePermissionService) {
        this.userService           = userService;
        this.groupService          = groupService;
        this.roleService           = roleService;
        this.rolePermissionService = rolePermissionService;
    }

    @McpResource(
        uri         = "rbac://user/context",
        description = "The authenticated user's profile, group memberships, "
                    + "roles, and effective permissions. Load once per session "
                    + "to understand the user's access state before acting."
    )
    public CompletableFuture<UserContext> load(RequestContext rc) {
        CompletableFuture<Optional<Rbac.User>> userFuture   = userService.get(rc, rc.getAuthContext().getUserId());
        CompletableFuture<List<Rbac.Group>>    groupsFuture = groupService.getUserGroups(rc);
        CompletableFuture<List<Rbac.Role>>     rolesFuture  = roleService.getUserRoles(rc);
        CompletableFuture<List<String>>        permsFuture  = rolePermissionService.getUserPermissions(rc);

        return CompletableFuture.allOf(userFuture, groupsFuture, rolesFuture, permsFuture)
            .thenApply(v -> new UserContext(
                userFuture.join().orElse(null),
                groupsFuture.join(),
                rolesFuture.join(),
                permsFuture.join()
            ));
    }

    public record UserContext(
        Rbac.User         user,
        List<Rbac.Group>  groups,
        List<Rbac.Role>   roles,
        List<String>      permissions
    ) {}
}
