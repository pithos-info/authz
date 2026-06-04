package info.pithos.rbac.impl;

import info.pithos.data.relational.RelationalClient;
import info.pithos.data.relational.Row;
import info.pithos.rbac.AbstractRbacService;
import info.pithos.rbac.ApiKeyService;
import info.pithos.rbac.model.Rbac;
import info.pithos.runtime.model.protocol.http.RequestContextOuterClass.RequestContext;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class RelationalApiKeyService extends AbstractRbacService implements ApiKeyService {

    public RelationalApiKeyService(RelationalClient relationalClient) {
        super(relationalClient);
    }

    @Override
    public CompletableFuture<Rbac.ApiKey> create(RequestContext rc, Rbac.ApiKey apiKey) {
        return relationalClient.query(rc,
            """
            INSERT INTO "apiKeys" ("enterpriseId", "userId", name, "keyHash", "keyPrefix", permissions, "expiresAt")
            VALUES (?, ?, ?, ?, ?, ?::text[], ?)
            RETURNING id, "enterpriseId", "userId", name, "keyHash", "keyPrefix", permissions, "expiresAt", "lastUsedAt", "utcCreatedAt"
            """,
            UUID.fromString(apiKey.getEnterpriseId()),
            UUID.fromString(apiKey.getUserId()),
            apiKey.getName(),
            apiKey.getKeyHash(),
            apiKey.getKeyPrefix(),
            pgArray(apiKey.getPermissionsList()),
            apiKey.getExpiresAt() > 0 ? new Timestamp(apiKey.getExpiresAt()) : null)
            .thenApply(rows -> toApiKey(rows.get(0)));
    }

    @Override
    public CompletableFuture<Optional<Rbac.ApiKey>> get(RequestContext rc, String id) {
        return relationalClient.query(rc,
            "SELECT id, \"enterpriseId\", \"userId\", name, \"keyHash\", \"keyPrefix\", permissions, \"expiresAt\", \"lastUsedAt\", \"utcCreatedAt\" FROM \"apiKeys\" WHERE id = ?",
            UUID.fromString(id))
            .thenApply(rows -> rows.isEmpty() ? Optional.empty() : Optional.of(toApiKey(rows.get(0))));
    }

    @Override
    public CompletableFuture<Void> revoke(RequestContext rc, String id) {
        return relationalClient.execute(rc,
            "DELETE FROM \"apiKeys\" WHERE id = ?", UUID.fromString(id))
            .thenAccept(n -> {});
    }

    @Override
    public CompletableFuture<List<Rbac.ApiKey>> list(RequestContext rc) {
        return relationalClient.query(rc,
            "SELECT id, \"enterpriseId\", \"userId\", name, \"keyHash\", \"keyPrefix\", permissions, \"expiresAt\", \"lastUsedAt\", \"utcCreatedAt\" FROM \"apiKeys\" WHERE \"enterpriseId\" = ? AND \"userId\" = ? ORDER BY \"utcCreatedAt\"",
            authEnterpriseId(rc), authUserId(rc))
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
