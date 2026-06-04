package info.pithos.rbac.impl;

import info.pithos.data.relational.ProtoBufStatement;
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

    private static final ProtoBufStatement<Rbac.Role> STMT =
        ProtoBufStatement.of("roles", Rbac.Role.getDefaultInstance(), new String[]{"deleted"});

    public RelationalRoleService(RelationalClient relationalClient) {
        super(relationalClient);
    }

    @Override
    public CompletableFuture<Rbac.Role> create(RequestContext rc, String name) {
        Rbac.Role role = Rbac.Role.newBuilder()
            .setId(UUID.randomUUID().toString())
            .setEnterpriseId(authEnterpriseId(rc).toString())
            .setName(name)
            .setUtcCreatedAt(System.currentTimeMillis())
            .build();
        return relationalClient.query(rc, STMT.insert(role))
            .thenApply(rows -> toRole(rows.get(0)));
    }

    @Override
    public CompletableFuture<Optional<Rbac.Role>> get(RequestContext rc, String id) {
        return relationalClient.query(rc, STMT.selectById(id))
            .thenApply(rows -> rows.isEmpty() ? Optional.empty() : Optional.of(toRole(rows.get(0))));
    }

    @Override
    public CompletableFuture<Rbac.Role> update(RequestContext rc, Rbac.Role role) {
        return relationalClient.query(rc, STMT.update(role))
            .thenApply(rows -> {
                if (rows.isEmpty()) throw new IllegalArgumentException("Role not found: " + role.getId());
                return toRole(rows.get(0));
            });
    }

    @Override
    public CompletableFuture<Void> delete(RequestContext rc, String id) {
        return relationalClient.execute(rc, STMT.softDelete(id)).thenAccept(n -> {});
    }

    @Override
    public CompletableFuture<List<Rbac.Role>> list(RequestContext rc) {
        String sql = "SELECT " + STMT.columnList()
            + " FROM \"roles\" WHERE \"enterpriseId\" = ? AND deleted = false ORDER BY name";
        return relationalClient.query(rc, sql, authEnterpriseId(rc))
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
