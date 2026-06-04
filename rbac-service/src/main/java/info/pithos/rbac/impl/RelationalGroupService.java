package info.pithos.rbac.impl;

import info.pithos.data.relational.RelationalClient;
import info.pithos.data.relational.Row;
import info.pithos.rbac.AbstractRbacService;
import info.pithos.rbac.GroupService;
import info.pithos.rbac.model.Rbac;
import info.pithos.runtime.model.protocol.http.RequestContextOuterClass.RequestContext;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class RelationalGroupService extends AbstractRbacService implements GroupService {

    public RelationalGroupService(RelationalClient relationalClient) {
        super(relationalClient);
    }

    @Override
    public CompletableFuture<Rbac.Group> create(RequestContext rc, String name) {
        return relationalClient.query(rc,
            "INSERT INTO groups (\"enterpriseId\", name) VALUES (?, ?) RETURNING id, \"enterpriseId\", name, \"utcCreatedAt\", deleted",
            authEnterpriseId(rc), name)
            .thenApply(rows -> toGroup(rows.get(0)));
    }

    @Override
    public CompletableFuture<Optional<Rbac.Group>> get(RequestContext rc, String id) {
        return relationalClient.query(rc,
            "SELECT id, \"enterpriseId\", name, \"utcCreatedAt\", deleted FROM groups WHERE id = ?",
            UUID.fromString(id))
            .thenApply(rows -> rows.isEmpty() ? Optional.empty() : Optional.of(toGroup(rows.get(0))));
    }

    @Override
    public CompletableFuture<Rbac.Group> update(RequestContext rc, Rbac.Group group) {
        return relationalClient.query(rc,
            "UPDATE groups SET name = ? WHERE id = ? RETURNING id, \"enterpriseId\", name, \"utcCreatedAt\", deleted",
            group.getName(), UUID.fromString(group.getId()))
            .thenApply(rows -> {
                if (rows.isEmpty()) throw new IllegalArgumentException("Group not found: " + group.getId());
                return toGroup(rows.get(0));
            });
    }

    @Override
    public CompletableFuture<Void> delete(RequestContext rc, String id) {
        return relationalClient.execute(rc,
            "UPDATE groups SET deleted = true WHERE id = ?", UUID.fromString(id))
            .thenAccept(n -> {});
    }

    @Override
    public CompletableFuture<List<Rbac.Group>> list(RequestContext rc) {
        return relationalClient.query(rc,
            "SELECT id, \"enterpriseId\", name, \"utcCreatedAt\", deleted FROM groups WHERE \"enterpriseId\" = ? AND deleted = false ORDER BY name",
            authEnterpriseId(rc))
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
