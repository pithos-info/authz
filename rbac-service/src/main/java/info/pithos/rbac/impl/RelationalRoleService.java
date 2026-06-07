package info.pithos.rbac.impl;

import info.pithos.data.relational.PreparedQuery;
import info.pithos.data.relational.client.ProtoBufRelationalClient;
import info.pithos.data.relational.client.RelationalClient;
import info.pithos.rbac.AbstractRbacService;
import info.pithos.rbac.RoleService;
import info.pithos.rbac.model.Rbac;
import info.pithos.runtime.model.protocol.Context.RequestContext;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class RelationalRoleService extends AbstractRbacService implements RoleService {

    private final ProtoBufRelationalClient<Rbac.Role> store;

    public RelationalRoleService(RelationalClient relationalClient) {
        super(relationalClient);
        this.store = ProtoBufRelationalClient.of(relationalClient, Rbac.Role.getDefaultInstance(), "deleted");
    }

    @Override
    public CompletableFuture<Rbac.Role> create(RequestContext rc, String name) {
        Rbac.Role role = Rbac.Role.newBuilder()
            .setId(UUID.randomUUID().toString())
            .setEnterpriseId(authEnterpriseId(rc).toString())
            .setName(name)
            .setUtcCreatedAt(System.currentTimeMillis())
            .build();
        return store.insert(dc(rc), role);
    }

    @Override
    public CompletableFuture<Optional<Rbac.Role>> get(RequestContext rc, String id) {
        return store.findById(dc(rc), id).thenApply(Optional::ofNullable);
    }

    @Override
    public CompletableFuture<Rbac.Role> update(RequestContext rc, Rbac.Role role) {
        return store.update(dc(rc), role);
    }

    @Override
    public CompletableFuture<Void> delete(RequestContext rc, String id) {
        return store.softDelete(dc(rc), id).thenAccept(n -> {});
    }

    @Override
    public CompletableFuture<List<Rbac.Role>> list(RequestContext rc) {
        String sql = "SELECT " + store.statement().columnList()
            + " FROM role WHERE \"enterpriseId\" = ? AND deleted = false ORDER BY name";
        return store.findAll(dc(rc), new PreparedQuery(sql, new Object[]{authEnterpriseId(rc)}));
    }

    @Override
    public CompletableFuture<List<Rbac.Role>> getUserRoles(RequestContext rc) {
        UUID uid = authUserId(rc);
        String sql = "SELECT DISTINCT " + store.statement().columnList()
            + " FROM role"
            + " WHERE id IN ("
            + "   SELECT \"roleId\" FROM \"userRole\" WHERE \"userId\" = ?"
            + "   UNION"
            + "   SELECT gr.\"roleId\" FROM \"groupRole\" gr"
            + "   JOIN \"groupMember\" gm ON gm.\"groupId\" = gr.\"groupId\""
            + "   WHERE gm.\"userId\" = ?"
            + " )"
            + " AND \"enterpriseId\" = ? AND deleted = false"
            + " ORDER BY name";
        return store.findAll(dc(rc), new PreparedQuery(sql, new Object[]{uid, uid, authEnterpriseId(rc)}));
    }
}
