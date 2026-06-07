package info.pithos.rbac.impl;

import info.pithos.data.relational.PreparedQuery;
import info.pithos.data.relational.client.ProtoBufRelationalClient;
import info.pithos.data.relational.client.RelationalClient;
import info.pithos.rbac.AbstractRbacService;
import info.pithos.rbac.GroupService;
import info.pithos.rbac.model.Rbac;
import info.pithos.runtime.model.protocol.Context.RequestContext;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class RelationalGroupService extends AbstractRbacService implements GroupService {

    private final ProtoBufRelationalClient<Rbac.Group> store;

    public RelationalGroupService(RelationalClient relationalClient) {
        super(relationalClient);
        this.store = ProtoBufRelationalClient.of(relationalClient, Rbac.Group.getDefaultInstance(), "deleted");
    }

    @Override
    public CompletableFuture<Rbac.Group> create(RequestContext rc, String name) {
        Rbac.Group group = Rbac.Group.newBuilder()
            .setId(java.util.UUID.randomUUID().toString())
            .setEnterpriseId(authEnterpriseId(rc).toString())
            .setName(name)
            .setUtcCreatedAt(System.currentTimeMillis())
            .build();
        return store.insert(dc(rc), group);
    }

    @Override
    public CompletableFuture<Optional<Rbac.Group>> get(RequestContext rc, String id) {
        return store.findById(dc(rc), id).thenApply(Optional::ofNullable);
    }

    @Override
    public CompletableFuture<Rbac.Group> update(RequestContext rc, Rbac.Group group) {
        return store.update(dc(rc), group);
    }

    @Override
    public CompletableFuture<Void> delete(RequestContext rc, String id) {
        return store.softDelete(dc(rc), id).thenAccept(n -> {});
    }

    @Override
    public CompletableFuture<List<Rbac.Group>> list(RequestContext rc) {
        String sql = "SELECT " + store.statement().columnList()
            + " FROM \"group\" WHERE \"enterpriseId\" = ? AND deleted = false ORDER BY name";
        return store.findAll(dc(rc), new PreparedQuery(sql, new Object[]{authEnterpriseId(rc)}));
    }

    @Override
    public CompletableFuture<List<Rbac.Group>> getUserGroups(RequestContext rc) {
        String sql = "SELECT " + store.statement().columnList()
            + " FROM \"group\""
            + " WHERE id IN (SELECT \"groupId\" FROM \"groupMember\" WHERE \"userId\" = ?)"
            + " AND \"enterpriseId\" = ? AND deleted = false"
            + " ORDER BY name";
        return store.findAll(dc(rc), new PreparedQuery(sql, new Object[]{authUserId(rc), authEnterpriseId(rc)}));
    }
}
