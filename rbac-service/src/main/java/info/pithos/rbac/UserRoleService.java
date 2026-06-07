package info.pithos.rbac;

import info.pithos.data.relational.FilterCriteria;
import info.pithos.rbac.model.Rbac;
import info.pithos.runtime.model.protocol.Context.RequestContext;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface UserRoleService {

    /** Grants a role to a user. The caller from authContext is recorded as grantedById. */
    CompletableFuture<Rbac.UserRole> grant(RequestContext rc, String userId, String roleId);

    /** Revokes a role from a user. */
    CompletableFuture<Void> revoke(RequestContext rc, String userId, String roleId);

    /** Returns the assignment if it exists. */
    CompletableFuture<Optional<Rbac.UserRole>> get(RequestContext rc, String userId, String roleId);

    /** Returns all assignments matching the given filter (e.g. {@code FilterCriteria.eq("userId", id)}). */
    CompletableFuture<List<Rbac.UserRole>> select(RequestContext rc, FilterCriteria filter);

    /** Returns {@code true} if {@code authContext.userId} holds {@code roleId} directly or via group membership. */
    CompletableFuture<Boolean> hasRole(RequestContext rc, String roleId);
}
