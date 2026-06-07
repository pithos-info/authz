package info.pithos.rbac;

import info.pithos.rbac.model.Rbac;
import info.pithos.runtime.model.protocol.Context.RequestContext;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface EnterpriseService {

    CompletableFuture<Rbac.Enterprise> create(RequestContext rc, Rbac.Enterprise enterprise);

    CompletableFuture<Optional<Rbac.Enterprise>> get(RequestContext rc, String id);

    CompletableFuture<Rbac.Enterprise> update(RequestContext rc, Rbac.Enterprise enterprise);

    CompletableFuture<Void> delete(RequestContext rc, String id);

    CompletableFuture<List<Rbac.Enterprise>> list(RequestContext rc);
}
