package info.pithos.rbac.impl;

import info.pithos.data.relational.ProtoBufStatement;
import info.pithos.data.relational.RelationalClient;
import info.pithos.data.relational.Row;
import info.pithos.rbac.AbstractRbacService;
import info.pithos.rbac.EnterpriseService;
import info.pithos.rbac.model.Rbac;
import info.pithos.runtime.model.protocol.http.RequestContextOuterClass.RequestContext;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class RelationalEnterpriseService extends AbstractRbacService implements EnterpriseService {

    private static final ProtoBufStatement<Rbac.Enterprise> STMT =
        ProtoBufStatement.of(Rbac.Enterprise.getDefaultInstance(), "deleted");

    public RelationalEnterpriseService(RelationalClient relationalClient) {
        super(relationalClient);
    }

    @Override
    public CompletableFuture<Rbac.Enterprise> create(RequestContext rc, Rbac.Enterprise enterprise) {
        return relationalClient.query(rc, STMT.insert(enterprise))
            .thenApply(rows -> toEnterprise(rows.get(0)));
    }

    @Override
    public CompletableFuture<Optional<Rbac.Enterprise>> get(RequestContext rc, String id) {
        return relationalClient.query(rc, STMT.selectById(id))
            .thenApply(rows -> rows.isEmpty() ? Optional.empty() : Optional.of(toEnterprise(rows.get(0))));
    }

    @Override
    public CompletableFuture<Rbac.Enterprise> update(RequestContext rc, Rbac.Enterprise enterprise) {
        return relationalClient.query(rc, STMT.update(enterprise))
            .thenApply(rows -> {
                if (rows.isEmpty()) throw new IllegalArgumentException("Enterprise not found: " + enterprise.getId());
                return toEnterprise(rows.get(0));
            });
    }

    @Override
    public CompletableFuture<Void> delete(RequestContext rc, String id) {
        return relationalClient.execute(rc, STMT.softDelete(id)).thenAccept(n -> {});
    }

    @Override
    public CompletableFuture<List<Rbac.Enterprise>> list(RequestContext rc) {
        String sql = "SELECT " + STMT.columnList()
            + " FROM \"enterprise\" WHERE deleted = false ORDER BY \"utcCreatedAt\"";
        return relationalClient.query(rc, sql)
            .thenApply(rows -> rows.stream().map(this::toEnterprise).toList());
    }

    private Rbac.Enterprise toEnterprise(Row row) {
        Rbac.Enterprise.Builder b = Rbac.Enterprise.newBuilder()
            .setId(row.getStr("id"))
            .setSlug(row.getString("slug"))
            .setName(row.getString("name"))
            .setPlan(row.getString("plan"))
            .setUtcCreatedAt(row.getEpochMillis("utcCreatedAt"))
            .setDeleted(row.getBoolOrFalse("deleted"));
        String domain = row.getString("domain");
        if (domain != null) b.setDomain(domain);
        return b.build();
    }
}
