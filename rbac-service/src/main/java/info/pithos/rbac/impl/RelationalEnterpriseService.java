package info.pithos.rbac.impl;

import info.pithos.data.relational.RelationalClient;
import info.pithos.data.relational.Row;
import info.pithos.rbac.AbstractRbacService;
import info.pithos.rbac.EnterpriseService;
import info.pithos.rbac.model.Rbac;
import info.pithos.runtime.model.protocol.http.RequestContextOuterClass.RequestContext;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class RelationalEnterpriseService extends AbstractRbacService implements EnterpriseService {

    public RelationalEnterpriseService(RelationalClient relationalClient) {
        super(relationalClient);
    }

    @Override
    public CompletableFuture<Rbac.Enterprise> create(RequestContext rc, Rbac.Enterprise enterprise) {
        return relationalClient.query(rc,
            """
            INSERT INTO enterprise (slug, name, plan, domain)
            VALUES (?, ?, ?, ?)
            RETURNING id, slug, name, plan, domain, "utcCreatedAt", deleted
            """,
            enterprise.getSlug(), enterprise.getName(), enterprise.getPlan(),
            enterprise.getDomain().isEmpty() ? null : enterprise.getDomain())
            .thenApply(rows -> toEnterprise(rows.get(0)));
    }

    @Override
    public CompletableFuture<Optional<Rbac.Enterprise>> get(RequestContext rc, String id) {
        return relationalClient.query(rc,
            "SELECT id, slug, name, plan, domain, \"utcCreatedAt\", deleted FROM enterprise WHERE id = ?",
            UUID.fromString(id))
            .thenApply(rows -> rows.isEmpty() ? Optional.empty() : Optional.of(toEnterprise(rows.get(0))));
    }

    @Override
    public CompletableFuture<Rbac.Enterprise> update(RequestContext rc, Rbac.Enterprise enterprise) {
        return relationalClient.query(rc,
            """
            UPDATE enterprise SET slug = ?, name = ?, plan = ?, domain = ?
            WHERE id = ?
            RETURNING id, slug, name, plan, domain, "utcCreatedAt", deleted
            """,
            enterprise.getSlug(), enterprise.getName(), enterprise.getPlan(),
            enterprise.getDomain().isEmpty() ? null : enterprise.getDomain(),
            UUID.fromString(enterprise.getId()))
            .thenApply(rows -> {
                if (rows.isEmpty()) throw new IllegalArgumentException("Enterprise not found: " + enterprise.getId());
                return toEnterprise(rows.get(0));
            });
    }

    @Override
    public CompletableFuture<Void> delete(RequestContext rc, String id) {
        return relationalClient.execute(rc,
            "UPDATE enterprise SET deleted = true WHERE id = ?", UUID.fromString(id))
            .thenAccept(n -> {});
    }

    @Override
    public CompletableFuture<List<Rbac.Enterprise>> list(RequestContext rc) {
        return relationalClient.query(rc,
            "SELECT id, slug, name, plan, domain, \"utcCreatedAt\", deleted FROM enterprise WHERE deleted = false ORDER BY \"utcCreatedAt\"")
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
