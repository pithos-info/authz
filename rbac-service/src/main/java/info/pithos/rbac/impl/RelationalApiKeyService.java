package info.pithos.rbac.impl;

import info.pithos.data.relational.ProtoBufStatement;
import info.pithos.data.relational.RelationalClient;
import info.pithos.data.relational.Row;
import info.pithos.rbac.AbstractRbacService;
import info.pithos.rbac.ApiKeyService;
import info.pithos.rbac.model.Rbac;
import info.pithos.runtime.model.protocol.http.RequestContextOuterClass.RequestContext;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class RelationalApiKeyService extends AbstractRbacService implements ApiKeyService {

    private static final ProtoBufStatement STMT =
        ProtoBufStatement.of("apiKeys", Rbac.ApiKey.getDefaultInstance());

    public RelationalApiKeyService(RelationalClient relationalClient) {
        super(relationalClient);
    }

    @Override
    public CompletableFuture<Rbac.ApiKey> create(RequestContext rc, Rbac.ApiKey apiKey) {
        return relationalClient.query(rc, STMT.insert(apiKey))
            .thenApply(rows -> toApiKey(rows.get(0)));
    }

    @Override
    public CompletableFuture<Optional<Rbac.ApiKey>> get(RequestContext rc, String id) {
        return relationalClient.query(rc, STMT.selectById(id))
            .thenApply(rows -> rows.isEmpty() ? Optional.empty() : Optional.of(toApiKey(rows.get(0))));
    }

    @Override
    public CompletableFuture<Void> revoke(RequestContext rc, String id) {
        return relationalClient.execute(rc, STMT.delete(id)).thenAccept(n -> {});
    }

    @Override
    public CompletableFuture<List<Rbac.ApiKey>> list(RequestContext rc) {
        String sql = "SELECT " + STMT.columnList()
            + " FROM \"apiKeys\" WHERE \"enterpriseId\" = ? AND \"userId\" = ? ORDER BY \"utcCreatedAt\"";
        return relationalClient.query(rc, sql, authEnterpriseId(rc), authUserId(rc))
            .thenApply(rows -> rows.stream().map(RelationalApiKeyService::toApiKey).toList());
    }

    private static Rbac.ApiKey toApiKey(Row row) {
        return Rbac.ApiKey.newBuilder()
            .setId(row.getStr("id"))
            .setEnterpriseId(row.getStr("enterpriseId"))
            .setUserId(row.getStr("userId"))
            .setName(row.getString("name"))
            .setKeyHash(row.getString("keyHash"))
            .setKeyPrefix(row.getString("keyPrefix"))
            .addAllPermissions(row.getStringList("permissions"))
            .setExpiresAt(row.getEpochMillis("expiresAt"))
            .setLastUsedAt(row.getEpochMillis("lastUsedAt"))
            .setUtcCreatedAt(row.getEpochMillis("utcCreatedAt"))
            .build();
    }
}
