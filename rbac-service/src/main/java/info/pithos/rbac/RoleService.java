package info.pithos.rbac;

import info.pithos.data.relational.client.CrudService;
import info.pithos.rbac.model.Rbac;
import info.pithos.runtime.model.protocol.Context.RequestContext;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface RoleService extends CrudService<Rbac.Role> {

    /** Lists non-deleted roles scoped to {@code authContext.enterpriseId}. */
    CompletableFuture<List<Rbac.Role>> list(RequestContext rc);

    /** Returns roles held by {@code authContext.userId} directly or via group membership. */
    CompletableFuture<List<Rbac.Role>> getUserRoles(RequestContext rc);
}
