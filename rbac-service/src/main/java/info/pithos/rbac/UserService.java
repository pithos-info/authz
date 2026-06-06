package info.pithos.rbac;

import info.pithos.rbac.model.Rbac;
import info.pithos.runtime.model.protocol.http.Context.RequestContext;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface UserService {

    /** Creates a user; {@code user.enterpriseId} must be set by the caller. */
    CompletableFuture<Rbac.User> create(RequestContext rc, Rbac.User user);

    CompletableFuture<Optional<Rbac.User>> get(RequestContext rc, String id);

    CompletableFuture<Rbac.User> update(RequestContext rc, Rbac.User user);

    CompletableFuture<Void> delete(RequestContext rc, String id);

    /** Lists non-deleted users scoped to {@code authContext.enterpriseId}. */
    CompletableFuture<List<Rbac.User>> list(RequestContext rc);
}
