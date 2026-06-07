package info.pithos.rbac.impl;

import info.pithos.data.relational.PreparedQuery;
import info.pithos.data.relational.client.ProtoBufRelationalClient;
import info.pithos.data.relational.client.RelationalClient;
import info.pithos.rbac.AbstractRbacService;
import info.pithos.rbac.UserService;
import info.pithos.rbac.model.Rbac;
import info.pithos.runtime.model.protocol.Context.RequestContext;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class RelationalUserService extends AbstractRbacService implements UserService {

    private final ProtoBufRelationalClient<Rbac.User> store;

    public RelationalUserService(RelationalClient relationalClient) {
        super(relationalClient);
        this.store = ProtoBufRelationalClient.of(relationalClient, Rbac.User.getDefaultInstance(), "deleted");
    }

    @Override
    public CompletableFuture<Rbac.User> create(RequestContext rc, Rbac.User user) {
        return store.insert(dc(rc), user);
    }

    @Override
    public CompletableFuture<Optional<Rbac.User>> get(RequestContext rc, String id) {
        return store.findById(dc(rc), id).thenApply(Optional::ofNullable);
    }

    @Override
    public CompletableFuture<Rbac.User> update(RequestContext rc, Rbac.User user) {
        return store.update(dc(rc), user);
    }

    @Override
    public CompletableFuture<Void> delete(RequestContext rc, String id) {
        return store.softDelete(dc(rc), id).thenAccept(n -> {});
    }

    @Override
    public CompletableFuture<List<Rbac.User>> list(RequestContext rc) {
        String sql = "SELECT " + store.statement().columnList()
            + " FROM \"user\" WHERE \"enterpriseId\" = ? AND deleted = false ORDER BY \"utcCreatedAt\"";
        return store.findAll(dc(rc), new PreparedQuery(sql, new Object[]{authEnterpriseId(rc)}));
    }

    @Override
    public CompletableFuture<List<Rbac.User>> getUsersInGroup(RequestContext rc, String groupId) {
        String sql = "SELECT " + store.statement().columnList()
            + " FROM \"user\""
            + " WHERE id IN (SELECT \"userId\" FROM \"groupMember\" WHERE \"groupId\" = ?)"
            + " AND deleted = false"
            + " ORDER BY email";
        return store.findAll(dc(rc), new PreparedQuery(sql, new Object[]{UUID.fromString(groupId)}));
    }
}
