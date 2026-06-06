package info.pithos.rbac;

import info.pithos.rbac.model.Rbac;
import info.pithos.runtime.model.protocol.http.Context.RequestContext;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface RoleService {

    /** Creates a role within {@code authContext.enterpriseId}. */
    CompletableFuture<Rbac.Role> create(RequestContext rc, String name);

    CompletableFuture<Optional<Rbac.Role>> get(RequestContext rc, String id);

    CompletableFuture<Rbac.Role> update(RequestContext rc, Rbac.Role role);

    CompletableFuture<Void> delete(RequestContext rc, String id);

    /** Lists non-deleted roles scoped to {@code authContext.enterpriseId}. */
    CompletableFuture<List<Rbac.Role>> list(RequestContext rc);
}
