package info.pithos.rbac.impl;

import info.pithos.data.relational.client.ProtoBufStatement;
import info.pithos.data.relational.client.RelationalClient;
import info.pithos.data.relational.Row;
import info.pithos.rbac.AbstractRbacService;
import info.pithos.rbac.GroupService;
import info.pithos.rbac.model.Rbac;
import info.pithos.runtime.model.protocol.Context.RequestContext;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class RelationalGroupService extends AbstractRbacService implements GroupService {

    private static final ProtoBufStatement<Rbac.Group> STMT =
        ProtoBufStatement.of("groups", Rbac.Group.getDefaultInstance(), new String[]{"deleted"});

    public RelationalGroupService(RelationalClient relationalClient) {
        super(relationalClient);
    }

    @Override
    public CompletableFuture<Rbac.Group> create(RequestContext rc, String name) {
        Rbac.Group group = Rbac.Group.newBuilder()
            .setId(UUID.randomUUID().toString())
            .setEnterpriseId(authEnterpriseId(rc).toString())
            .setName(name)
            .setUtcCreatedAt(System.currentTimeMillis())
            .build();
        return relationalClient.query(dc(rc),STMT.insert(group))
            .thenApply(rows -> toGroup(rows.get(0)));
    }

    @Override
    public CompletableFuture<Optional<Rbac.Group>> get(RequestContext rc, String id) {
        return relationalClient.query(dc(rc),STMT.selectById(id))
            .thenApply(rows -> rows.isEmpty() ? Optional.empty() : Optional.of(toGroup(rows.get(0))));
    }

    @Override
    public CompletableFuture<Rbac.Group> update(RequestContext rc, Rbac.Group group) {
        return relationalClient.query(dc(rc),STMT.update(group))
            .thenApply(rows -> {
                if (rows.isEmpty()) throw new IllegalArgumentException("Group not found: " + group.getId());
                return toGroup(rows.get(0));
            });
    }

    @Override
    public CompletableFuture<Void> delete(RequestContext rc, String id) {
        return relationalClient.execute(dc(rc),STMT.softDelete(id)).thenAccept(n -> {});
    }

    @Override
    public CompletableFuture<List<Rbac.Group>> list(RequestContext rc) {
        String sql = "SELECT " + STMT.columnList()
            + " FROM \"groups\" WHERE \"enterpriseId\" = ? AND deleted = false ORDER BY name";
        return relationalClient.query(dc(rc),sql, authEnterpriseId(rc))
            .thenApply(rows -> rows.stream().map(RelationalGroupService::toGroup).toList());
    }

    static Rbac.Group toGroup(Row row) {
        return Rbac.Group.newBuilder()
            .setId(row.getStr("id"))
            .setEnterpriseId(row.getStr("enterpriseId"))
            .setName(row.getString("name"))
            .setUtcCreatedAt(row.getEpochMillis("utcCreatedAt"))
            .setDeleted(row.getBoolOrFalse("deleted"))
            .build();
    }
}
