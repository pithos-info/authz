package info.pithos.rbac.impl;

import info.pithos.data.relational.client.ProtoBufStatement;
import info.pithos.data.relational.client.RelationalClient;
import info.pithos.data.relational.Row;
import info.pithos.rbac.AbstractRbacService;
import info.pithos.rbac.UserService;
import info.pithos.rbac.model.Rbac;
import info.pithos.runtime.model.protocol.Context.RequestContext;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class RelationalUserService extends AbstractRbacService implements UserService {

    private static final ProtoBufStatement<Rbac.User> STMT =
        ProtoBufStatement.of(Rbac.User.getDefaultInstance(), "deleted");

    public RelationalUserService(RelationalClient relationalClient) {
        super(relationalClient);
    }

    @Override
    public CompletableFuture<Rbac.User> create(RequestContext rc, Rbac.User user) {
        return relationalClient.query(dc(rc),STMT.insert(user))
            .thenApply(rows -> toUser(rows.get(0)));
    }

    @Override
    public CompletableFuture<Optional<Rbac.User>> get(RequestContext rc, String id) {
        return relationalClient.query(dc(rc),STMT.selectById(id))
            .thenApply(rows -> rows.isEmpty() ? Optional.empty() : Optional.of(toUser(rows.get(0))));
    }

    @Override
    public CompletableFuture<Rbac.User> update(RequestContext rc, Rbac.User user) {
        return relationalClient.query(dc(rc),STMT.update(user))
            .thenApply(rows -> {
                if (rows.isEmpty()) throw new IllegalArgumentException("User not found: " + user.getId());
                return toUser(rows.get(0));
            });
    }

    @Override
    public CompletableFuture<Void> delete(RequestContext rc, String id) {
        return relationalClient.execute(dc(rc),STMT.softDelete(id)).thenAccept(n -> {});
    }

    @Override
    public CompletableFuture<List<Rbac.User>> list(RequestContext rc) {
        String sql = "SELECT " + STMT.columnList()
            + " FROM \"user\" WHERE \"enterpriseId\" = ? AND deleted = false ORDER BY \"utcCreatedAt\"";
        return relationalClient.query(dc(rc),sql, authEnterpriseId(rc))
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
