package info.pithos.rbac.impl;

import info.pithos.data.relational.PreparedQuery;
import info.pithos.rbac.ApiKeyService;
import info.pithos.data.relational.client.RelationalClient;
import info.pithos.data.relational.client.ProtoBufCrudService;
import info.pithos.rbac.model.Rbac;
import info.pithos.runtime.model.protocol.Context.RequestContext;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class RelationalApiKeyService extends ProtoBufCrudService<Rbac.ApiKey> implements ApiKeyService {

    public RelationalApiKeyService(RelationalClient relationalClient) {
        super(relationalClient, "apiKey", Rbac.ApiKey.getDefaultInstance());
    }

    @Override
    public CompletableFuture<Rbac.ApiKey> create(RequestContext rc, Rbac.ApiKey apiKey) {
        return save(rc, apiKey);
    }

    @Override
    public CompletableFuture<Void> revoke(RequestContext rc, String id) {
        return erase(rc, id);
    }

    @Override
    public CompletableFuture<List<Rbac.ApiKey>> list(RequestContext rc) {
        String sql = "SELECT " + store.statement().columnList()
            + " FROM \"apiKey\" WHERE \"enterpriseId\" = ? AND \"userId\" = ? ORDER BY \"utcCreatedAt\"";
        return query(rc, new PreparedQuery(sql, new Object[]{authEnterpriseId(rc), authUserId(rc)}));
    }

    @Override
    public CompletableFuture<Optional<Rbac.ApiKey>> findByKeyHash(RequestContext rc, String keyHash) {
        String sql = "SELECT " + store.statement().columnList()
            + " FROM \"apiKey\" WHERE \"keyHash\" = ?";
        return query(rc, new PreparedQuery(sql, new Object[]{keyHash}))
            .thenApply(list -> list.isEmpty() ? Optional.empty() : Optional.of(list.get(0)));
    }

    @Override
    public CompletableFuture<Void> touch(RequestContext rc, String id) {
        String sql = "UPDATE \"apiKey\" SET \"lastUsedAt\" = now() WHERE id = ?";
        return relationalClient.execute(dc(rc), new PreparedQuery(sql, new Object[]{id}))
            .thenAccept(n -> {});
    }
}
