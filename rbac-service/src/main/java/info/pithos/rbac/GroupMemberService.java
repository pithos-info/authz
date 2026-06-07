package info.pithos.rbac;

import info.pithos.data.relational.FilterCriteria;
import info.pithos.rbac.model.Rbac;
import info.pithos.runtime.model.protocol.Context.RequestContext;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface GroupMemberService {

    /** Adds a user to a group. Returns the created membership. */
    CompletableFuture<Rbac.GroupMember> add(RequestContext rc, String groupId, String userId);

    /** Removes a user from a group. */
    CompletableFuture<Void> remove(RequestContext rc, String groupId, String userId);

    /** Returns the membership if it exists. */
    CompletableFuture<Optional<Rbac.GroupMember>> get(RequestContext rc, String groupId, String userId);

    /** Returns all memberships matching the given filter (e.g. {@code FilterCriteria.eq("groupId", id)}). */
    CompletableFuture<List<Rbac.GroupMember>> select(RequestContext rc, FilterCriteria filter);

    /** Returns {@code true} if {@code authContext.userId} is a member of {@code groupId} within {@code authContext.enterpriseId}. */
    CompletableFuture<Boolean> isUserInGroup(RequestContext rc, String groupId);
}
