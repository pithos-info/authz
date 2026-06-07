package info.pithos.rbac.impl;

import info.pithos.data.relational.PreparedQuery;
import info.pithos.data.relational.client.ProtoBufRelationalClient;
import info.pithos.data.relational.client.RelationalClient;
import info.pithos.rbac.AbstractRbacService;
import info.pithos.rbac.EnterpriseService;
import info.pithos.rbac.model.Rbac;
import info.pithos.runtime.model.protocol.Context.RequestContext;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class RelationalEnterpriseService extends AbstractRbacService implements EnterpriseService {

    private final ProtoBufRelationalClient<Rbac.Enterprise> store;

    public RelationalEnterpriseService(RelationalClient relationalClient) {
        super(relationalClient);
        this.store = ProtoBufRelationalClient.of(relationalClient, Rbac.Enterprise.getDefaultInstance(), "deleted");
    }

    @Override
    public CompletableFuture<Rbac.Enterprise> create(RequestContext rc, Rbac.Enterprise enterprise) {
        return store.insert(dc(rc), enterprise);
    }

    @Override
    public CompletableFuture<Optional<Rbac.Enterprise>> get(RequestContext rc, String id) {
        return store.findById(dc(rc), id).thenApply(Optional::ofNullable);
    }

    @Override
    public CompletableFuture<Rbac.Enterprise> update(RequestContext rc, Rbac.Enterprise enterprise) {
        return store.update(dc(rc), enterprise);
    }

    @Override
    public CompletableFuture<Void> delete(RequestContext rc, String id) {
        return store.softDelete(dc(rc), id).thenAccept(n -> {});
    }

    @Override
    public CompletableFuture<List<Rbac.Enterprise>> list(RequestContext rc) {
        String sql = "SELECT " + store.statement().columnList()
            + " FROM \"enterprise\" WHERE deleted = false ORDER BY \"utcCreatedAt\"";
        return store.findAll(dc(rc), new PreparedQuery(sql, new Object[0]));
    }
}
