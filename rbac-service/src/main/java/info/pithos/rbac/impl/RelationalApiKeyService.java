package info.pithos.rbac.impl;

import info.pithos.data.relational.PreparedQuery;
import info.pithos.data.relational.client.ProtoBufRelationalClient;
import info.pithos.data.relational.client.RelationalClient;
import info.pithos.rbac.AbstractRbacService;
import info.pithos.rbac.ApiKeyService;
import info.pithos.rbac.model.Rbac;
import info.pithos.runtime.model.protocol.Context.RequestContext;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class RelationalApiKeyService extends AbstractRbacService implements ApiKeyService {

    private final ProtoBufRelationalClient<Rbac.ApiKey> store;

    public RelationalApiKeyService(RelationalClient relationalClient) {
        super(relationalClient);
        this.store = ProtoBufRelationalClient.of(relationalClient, "apiKey", Rbac.ApiKey.getDefaultInstance());
    }

    @Override
    public CompletableFuture<Rbac.ApiKey> create(RequestContext rc, Rbac.ApiKey apiKey) {
        return store.insert(dc(rc), apiKey);
    }

    @Override
    public CompletableFuture<Optional<Rbac.ApiKey>> get(RequestContext rc, String id) {
        return store.findById(dc(rc), id).thenApply(Optional::ofNullable);
    }

    @Override
    public CompletableFuture<Void> revoke(RequestContext rc, String id) {
        return store.delete(dc(rc), id).thenAccept(n -> {});
    }

    @Override
    public CompletableFuture<List<Rbac.ApiKey>> list(RequestContext rc) {
        String sql = "SELECT " + store.statement().columnList()
            + " FROM \"apiKey\" WHERE \"enterpriseId\" = ? AND \"userId\" = ? ORDER BY \"utcCreatedAt\"";
        return store.findAll(dc(rc), new PreparedQuery(sql, new Object[]{authEnterpriseId(rc), authUserId(rc)}));
    }
}
