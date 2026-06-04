package info.pithos.rbac.impl;

import info.pithos.data.relational.RelationalClient;
import info.pithos.data.relational.Row;
import info.pithos.rbac.AbstractRbacService;
import info.pithos.rbac.RoleService;
import info.pithos.rbac.model.Rbac;
import info.pithos.runtime.model.protocol.http.RequestContextOuterClass.RequestContext;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class RelationalRoleService extends AbstractRbacService implements RoleService {

    public RelationalRoleService(RelationalClient relationalClient) {
        super(relationalClient);
    }

    @Override
    public CompletableFuture<Rbac.Role> create(RequestContext rc, String name) {
        return relationalClient.query(rc,
            "INSERT INTO roles (\"enterpriseId\", name) VALUES (?, ?) RETURNING id, \"enterpriseId\", name, \"utcCreatedAt\", deleted",
            authEnterpriseId(rc), name)
            .thenApply(rows -> toRole(rows.get(0)));
    }

    @Override
    public CompletableFuture<Optional<Rbac.Role>> get(RequestContext rc, String id) {
        return relationalClient.query(rc,
            "SELECT id, \"enterpriseId\", name, \"utcCreatedAt\", deleted FROM roles WHERE id = ?",
            UUID.fromString(id))
            .thenApply(rows -> rows.isEmpty() ? Optional.empty() : Optional.of(toRole(rows.get(0))));
    }

    @Override
    public CompletableFuture<Rbac.Role> update(RequestContext rc, Rbac.Role role) {
        return relationalClient.query(rc,
            "UPDATE roles SET name = ? WHERE id = ? RETURNING id, \"enterpriseId\", name, \"utcCreatedAt\", deleted",
            role.getName(), UUID.fromString(role.getId()))
            .thenApply(rows -> {
                if (rows.isEmpty()) throw new IllegalArgumentException("Role not found: " + role.getId());
                return toRole(rows.get(0));
            });
    }

    @Override
    public CompletableFuture<Void> delete(RequestContext rc, String id) {
        return relationalClient.execute(rc,
            "UPDATE roles SET deleted = true WHERE id = ?", UUID.fromString(id))
            .thenAccept(n -> {});
    }

    @Override
    public CompletableFuture<List<Rbac.Role>> list(RequestContext rc) {
        return relationalClient.query(rc,
            "SELECT id, \"enterpriseId\", name, \"utcCreatedAt\", deleted FROM roles WHERE \"enterpriseId\" = ? AND deleted = false ORDER BY name",
            authEnterpriseId(rc))
            .thenApply(rows -> rows.stream().map(RelationalRoleService::toRole).toList());
    }

    static Rbac.Role toRole(Row row) {
        return Rbac.Role.newBuilder()
            .setId(row.getStr("id"))
            .setEnterpriseId(row.getStr("enterpriseId"))
            .setName(row.getString("name"))
            .setUtcCreatedAt(row.getEpochMillis("utcCreatedAt"))
            .setDeleted(row.getBoolOrFalse("deleted"))
            .build();
    }
}
