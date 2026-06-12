package info.pithos.rbac;

import info.pithos.data.relational.client.CrudService;
import info.pithos.rbac.model.Rbac;
import info.pithos.runtime.model.protocol.Context.RequestContext;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface GroupService extends CrudService<Rbac.Group> {

    /** Lists non-deleted groups scoped to {@code authContext.enterpriseId}. */
    CompletableFuture<List<Rbac.Group>> list(RequestContext rc);

    /** Returns non-deleted groups that {@code authContext.userId} belongs to within {@code authContext.enterpriseId}. */
    CompletableFuture<List<Rbac.Group>> getUserGroups(RequestContext rc);
}
