package info.pithos.rbac;

import info.pithos.data.relational.client.CrudService;
import info.pithos.rbac.model.Rbac;
import info.pithos.runtime.model.protocol.Context.RequestContext;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface UserService extends CrudService<Rbac.User> {

    /** Lists non-deleted users scoped to {@code authContext.enterpriseId}. */
    CompletableFuture<List<Rbac.User>> list(RequestContext rc);

    /** Returns non-deleted users who are members of {@code groupId}, ordered by email. */
    CompletableFuture<List<Rbac.User>> getUsersInGroup(RequestContext rc, String groupId);

    /** Finds a non-deleted user by IdP subject within {@code authContext.enterpriseId}. */
    CompletableFuture<Optional<Rbac.User>> findByExternalId(RequestContext rc, String externalId);
}
