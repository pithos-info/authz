package info.pithos.rbac.impl;

import info.pithos.data.relational.RelationalClient;
import info.pithos.data.relational.Row;
import info.pithos.rbac.AbstractRbacService;
import info.pithos.rbac.UserService;
import info.pithos.rbac.model.Rbac;
import info.pithos.runtime.model.protocol.http.RequestContextOuterClass.RequestContext;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class RelationalUserService extends AbstractRbacService implements UserService {

    public RelationalUserService(RelationalClient relationalClient) {
        super(relationalClient);
    }

    @Override
    public CompletableFuture<Rbac.User> create(RequestContext rc, Rbac.User user) {
        return relationalClient.query(rc,
            """
            INSERT INTO "user" ("enterpriseId", email, "externalId", "idpProvider", "displayName")
            VALUES (?, ?, ?, ?, ?)
            RETURNING id, "enterpriseId", email, "externalId", "idpProvider", "displayName", "lastLoginAt", "utcCreatedAt", deleted
            """,
            UUID.fromString(user.getEnterpriseId()),
            user.getEmail(),
            user.getExternalId(),
            user.getIdpProvider(),
            user.getDisplayName().isEmpty() ? null : user.getDisplayName())
            .thenApply(rows -> toUser(rows.get(0)));
    }

    @Override
    public CompletableFuture<Optional<Rbac.User>> get(RequestContext rc, String id) {
        return relationalClient.query(rc,
            "SELECT id, \"enterpriseId\", email, \"externalId\", \"idpProvider\", \"displayName\", \"lastLoginAt\", \"utcCreatedAt\", deleted FROM \"user\" WHERE id = ?",
            UUID.fromString(id))
            .thenApply(rows -> rows.isEmpty() ? Optional.empty() : Optional.of(toUser(rows.get(0))));
    }

    @Override
    public CompletableFuture<Rbac.User> update(RequestContext rc, Rbac.User user) {
        return relationalClient.query(rc,
            """
            UPDATE "user" SET email = ?, "displayName" = ?, "idpProvider" = ?, "lastLoginAt" = ?
            WHERE id = ?
            RETURNING id, "enterpriseId", email, "externalId", "idpProvider", "displayName", "lastLoginAt", "utcCreatedAt", deleted
            """,
            user.getEmail(),
            user.getDisplayName().isEmpty() ? null : user.getDisplayName(),
            user.getIdpProvider(),
            user.getLastLoginAt() > 0 ? new Timestamp(user.getLastLoginAt()) : null,
            UUID.fromString(user.getId()))
            .thenApply(rows -> {
                if (rows.isEmpty()) throw new IllegalArgumentException("User not found: " + user.getId());
                return toUser(rows.get(0));
            });
    }

    @Override
    public CompletableFuture<Void> delete(RequestContext rc, String id) {
        return relationalClient.execute(rc,
            "UPDATE \"user\" SET deleted = true WHERE id = ?", UUID.fromString(id))
            .thenAccept(n -> {});
    }

    @Override
    public CompletableFuture<List<Rbac.User>> list(RequestContext rc) {
        return relationalClient.query(rc,
            "SELECT id, \"enterpriseId\", email, \"externalId\", \"idpProvider\", \"displayName\", \"lastLoginAt\", \"utcCreatedAt\", deleted FROM \"user\" WHERE \"enterpriseId\" = ? AND deleted = false ORDER BY \"utcCreatedAt\"",
            authEnterpriseId(rc))
            .thenApply(rows -> rows.stream().map(RelationalUserService::toUser).toList());
    }

    static Rbac.User toUser(Row row) {
        Rbac.User.Builder b = Rbac.User.newBuilder()
            .setId(row.getStr("id"))
            .setEnterpriseId(row.getStr("enterpriseId"))
            .setEmail(row.getString("email"))
            .setExternalId(row.getString("externalId"))
            .setIdpProvider(row.getString("idpProvider"))
            .setLastLoginAt(row.getEpochMillis("lastLoginAt"))
            .setUtcCreatedAt(row.getEpochMillis("utcCreatedAt"))
            .setDeleted(row.getBoolOrFalse("deleted"));
        String dn = row.getString("displayName");
        if (dn != null) b.setDisplayName(dn);
        return b.build();
    }
}
