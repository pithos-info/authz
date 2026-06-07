package info.pithos.rbac;

import info.pithos.rbac.model.Rbac;
import info.pithos.runtime.core.context.ServiceLifeCycle;
import info.pithos.runtime.model.protocol.Context.RequestContext;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Cross-cutting RBAC operations: group membership, role assignment, role permissions,
 * and permission/role helper queries. All methods derive the calling principal from
 * {@code requestContext.getAuthContext()}.
 */
public interface RbacService extends ServiceLifeCycle {

    // --- Group membership ---

    CompletableFuture<Void> addUserToGroup(RequestContext rc, String groupId, String userId);

    CompletableFuture<Void> removeUserFromGroup(RequestContext rc, String groupId, String userId);

    CompletableFuture<List<Rbac.User>> getGroupMembers(RequestContext rc, String groupId);

    /** Returns groups that {@code authContext.userId} belongs to within {@code authContext.enterpriseId}. */
    CompletableFuture<List<Rbac.Group>> getUserGroups(RequestContext rc);

    /** Returns {@code true} if {@code authContext.userId} is a member of {@code groupId}. */
    CompletableFuture<Boolean> isUserInGroup(RequestContext rc, String groupId);

    // --- Role assignment ---

    /** Assigns a role to a user; the granting principal is taken from {@code authContext.userId}. */
    CompletableFuture<Void> assignRoleToUser(RequestContext rc, String userId, String roleId);

    CompletableFuture<Void> revokeRoleFromUser(RequestContext rc, String userId, String roleId);

    /** Returns roles held by {@code authContext.userId} (directly or via groups). */
    CompletableFuture<List<Rbac.Role>> getUserRoles(RequestContext rc);

    CompletableFuture<Void> assignRoleToGroup(RequestContext rc, String groupId, String roleId);

    CompletableFuture<Void> revokeRoleFromGroup(RequestContext rc, String groupId, String roleId);

    // --- Role permissions ---

    CompletableFuture<Void> addPermissionToRole(RequestContext rc, String roleId, String permission);

    CompletableFuture<Void> removePermissionFromRole(RequestContext rc, String roleId, String permission);

    CompletableFuture<List<String>> getRolePermissions(RequestContext rc, String roleId);

    // --- Helpers (scoped to authContext.enterpriseId / authContext.userId) ---

    /** Returns {@code true} if {@code authContext.userId} holds the given permission (directly or via groups). */
    CompletableFuture<Boolean> hasPermission(RequestContext rc, String permission);

    /** Returns {@code true} if {@code authContext.userId} holds the given role (directly or via groups). */
    CompletableFuture<Boolean> hasRole(RequestContext rc, String roleId);

    /** Returns all distinct permissions effective for {@code authContext.userId}. */
    CompletableFuture<List<String>> getUserPermissions(RequestContext rc);
}
