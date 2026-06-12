package info.pithos.rbac.mcp.tools;

import com.google.inject.Inject;
import info.pithos.rbac.GroupMemberService;
import info.pithos.rbac.GroupService;
import info.pithos.rbac.UserRoleService;
import info.pithos.rbac.mcp.sdk.McpTool;
import info.pithos.rbac.model.Rbac;
import info.pithos.runtime.model.protocol.Context.RequestContext;

import java.util.concurrent.CompletableFuture;

/**
 * MCP tools for intent-level access management.
 *
 * These are write tools — only expose them to agents that are explicitly trusted to
 * modify access (e.g. an onboarding agent, an access-review agent). Agents that only
 * need to check or read access should use {@link AuthzTools} and {@link IdentityTools}.
 *
 * Authorization: each tool should verify the calling agent has the appropriate
 * administrative permission before delegating to the service layer (via hasPermission).
 * TODO: add permission guard once a standard admin permission string is established.
 */
public class AccessManagementTools {

    private final UserRoleService    userRoleService;
    private final GroupMemberService groupMemberService;
    private final GroupService       groupService;

    @Inject
    public AccessManagementTools(UserRoleService userRoleService,
                                  GroupMemberService groupMemberService,
                                  GroupService groupService) {
        this.userRoleService    = userRoleService;
        this.groupMemberService = groupMemberService;
        this.groupService       = groupService;
    }

    @McpTool(
        name        = "grant_role",
        description = "Grants a role directly to a user. "
                    + "Requires the calling agent to hold an administrative permission."
    )
    public CompletableFuture<Rbac.UserRole> grantRole(RequestContext rc,
                                                       String userId, String roleId) {
        // TODO: guard — verify rc holds admin permission before proceeding
        return userRoleService.grant(rc, userId, roleId);
    }

    @McpTool(
        name        = "revoke_role",
        description = "Revokes a directly-assigned role from a user."
    )
    public CompletableFuture<Void> revokeRole(RequestContext rc,
                                               String userId, String roleId) {
        // TODO: guard — verify rc holds admin permission before proceeding
        return userRoleService.revoke(rc, userId, roleId);
    }

    @McpTool(
        name        = "add_to_group",
        description = "Adds a user to a group, granting all roles assigned to that group."
    )
    public CompletableFuture<Rbac.GroupMember> addToGroup(RequestContext rc,
                                                           String groupId, String userId) {
        // TODO: guard — verify rc holds admin permission before proceeding
        return groupMemberService.add(rc, groupId, userId);
    }

    @McpTool(
        name        = "remove_from_group",
        description = "Removes a user from a group."
    )
    public CompletableFuture<Void> removeFromGroup(RequestContext rc,
                                                    String groupId, String userId) {
        // TODO: guard — verify rc holds admin permission before proceeding
        return groupMemberService.remove(rc, groupId, userId);
    }
}
