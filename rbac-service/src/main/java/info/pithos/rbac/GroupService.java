package info.pithos.rbac;

import info.pithos.rbac.model.Rbac;
import info.pithos.runtime.model.protocol.Context.RequestContext;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface GroupService {

    /** Creates a group within {@code authContext.enterpriseId}. */
    CompletableFuture<Rbac.Group> create(RequestContext rc, String name);

    CompletableFuture<Optional<Rbac.Group>> get(RequestContext rc, String id);

    CompletableFuture<Rbac.Group> update(RequestContext rc, Rbac.Group group);

    CompletableFuture<Void> delete(RequestContext rc, String id);

    /** Lists non-deleted groups scoped to {@code authContext.enterpriseId}. */
    CompletableFuture<List<Rbac.Group>> list(RequestContext rc);
}
