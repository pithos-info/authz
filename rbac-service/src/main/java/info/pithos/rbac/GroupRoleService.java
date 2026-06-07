package info.pithos.rbac;

import info.pithos.data.relational.FilterCriteria;
import info.pithos.rbac.model.Rbac;
import info.pithos.runtime.model.protocol.Context.RequestContext;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface GroupRoleService {

    /** Assigns a role to a group. */
    CompletableFuture<Rbac.GroupRole> assign(RequestContext rc, String groupId, String roleId);

    /** Unassigns a role from a group. */
    CompletableFuture<Void> unassign(RequestContext rc, String groupId, String roleId);

    /** Returns the assignment if it exists. */
    CompletableFuture<Optional<Rbac.GroupRole>> get(RequestContext rc, String groupId, String roleId);

    /** Returns all assignments matching the given filter (e.g. {@code FilterCriteria.eq("groupId", id)}). */
    CompletableFuture<List<Rbac.GroupRole>> select(RequestContext rc, FilterCriteria filter);
}
