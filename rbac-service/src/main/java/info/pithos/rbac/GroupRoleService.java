package info.pithos.rbac;

import info.pithos.data.relational.client.AssociationService;
import info.pithos.rbac.model.Rbac;
import info.pithos.runtime.model.protocol.Context.RequestContext;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface GroupRoleService extends AssociationService<Rbac.GroupRole> {

    /** Assigns a role to a group. */
    CompletableFuture<Rbac.GroupRole> assign(RequestContext rc, String groupId, String roleId);

    /** Unassigns a role from a group. */
    CompletableFuture<Void> unassign(RequestContext rc, String groupId, String roleId);

    /** Returns the assignment if it exists. */
    CompletableFuture<Optional<Rbac.GroupRole>> get(RequestContext rc, String groupId, String roleId);
}
