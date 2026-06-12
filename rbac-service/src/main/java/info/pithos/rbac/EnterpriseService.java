package info.pithos.rbac;

import info.pithos.data.relational.client.CrudService;
import info.pithos.rbac.model.Rbac;
import info.pithos.runtime.model.protocol.Context.RequestContext;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface EnterpriseService extends CrudService<Rbac.Enterprise> {

    CompletableFuture<List<Rbac.Enterprise>> list(RequestContext rc);
}
