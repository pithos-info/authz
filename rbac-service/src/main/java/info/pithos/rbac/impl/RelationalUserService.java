package info.pithos.rbac.impl;

import info.pithos.data.relational.PreparedQuery;
import info.pithos.data.relational.client.RelationalClient;
import info.pithos.data.relational.client.ProtoBufCrudService;
import info.pithos.rbac.UserService;
import info.pithos.rbac.model.Rbac;
import info.pithos.runtime.model.protocol.Context.RequestContext;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class RelationalUserService extends ProtoBufCrudService<Rbac.User> implements UserService {

    public RelationalUserService(RelationalClient relationalClient) {
        super(relationalClient, Rbac.User.getDefaultInstance(), "deleted");
    }

    @Override
    public CompletableFuture<Rbac.User> create(RequestContext rc, Rbac.User user) {
        return save(rc, user);
    }

    @Override
    public CompletableFuture<Rbac.User> update(RequestContext rc, Rbac.User user) {
        return merge(rc, user);
    }

    @Override
    public CompletableFuture<Void> delete(RequestContext rc, String id) {
        return remove(rc, id);
    }

    @Override
    public CompletableFuture<List<Rbac.User>> list(RequestContext rc) {
        String sql = "SELECT " + store.statement().columnList()
            + " FROM \"user\" WHERE \"enterpriseId\" = ? AND deleted = false ORDER BY \"utcCreatedAt\"";
        return query(rc, new PreparedQuery(sql, new Object[]{authEnterpriseId(rc)}));
    }

    @Override
    public CompletableFuture<List<Rbac.User>> getUsersInGroup(RequestContext rc, String groupId) {
        String sql = "SELECT " + store.statement().columnList()
            + " FROM \"user\""
            + " WHERE id IN (SELECT \"userId\" FROM \"groupMember\" WHERE \"groupId\" = ?)"
            + " AND deleted = false"
            + " ORDER BY email";
        return query(rc, new PreparedQuery(sql, new Object[]{groupId}));
    }

    @Override
    public CompletableFuture<Optional<Rbac.User>> findByExternalId(RequestContext rc, String externalId) {
        String sql = "SELECT " + store.statement().columnList()
            + " FROM \"user\" WHERE \"enterpriseId\" = ? AND \"externalId\" = ? AND deleted = false";
        return query(rc, new PreparedQuery(sql, new Object[]{authEnterpriseId(rc), externalId}))
            .thenApply(users -> users.isEmpty() ? Optional.empty() : Optional.of(users.get(0)));
    }
}
