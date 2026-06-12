package info.pithos.rbac.impl;

import info.pithos.data.cache.DistributedCacheClient;
import info.pithos.data.relational.PreparedQuery;
import info.pithos.data.relational.client.RelationalClient;
import info.pithos.data.relational.client.ProtoBufCrudService;
import info.pithos.rbac.RoleService;
import info.pithos.rbac.model.Rbac;
import info.pithos.runtime.core.context.AsyncTaskQueue;
import info.pithos.runtime.model.protocol.Context.RequestContext;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class RelationalRoleService extends ProtoBufCrudService<Rbac.Role> implements RoleService {

    public RelationalRoleService(RelationalClient relationalClient,
                                  DistributedCacheClient cacheClient,
                                  AsyncTaskQueue taskQueue) {
        super(relationalClient, cacheClient, taskQueue, Rbac.Role.getDefaultInstance(), "deleted");
    }

    @Override
    public CompletableFuture<Rbac.Role> create(RequestContext rc, String name) {
        Rbac.Role role = Rbac.Role.newBuilder()
            .setId(UUID.randomUUID().toString())
            .setEnterpriseId(authEnterpriseId(rc))
            .setName(name)
            .build();
        return save(rc, role);
    }

    @Override
    public CompletableFuture<Rbac.Role> update(RequestContext rc, Rbac.Role role) {
        return merge(rc, role);
    }

    @Override
    public CompletableFuture<Void> delete(RequestContext rc, String id) {
        return remove(rc, id);
    }

    @Override
    public CompletableFuture<List<Rbac.Role>> list(RequestContext rc) {
        String sql = "SELECT " + store.statement().columnList()
            + " FROM role WHERE \"enterpriseId\" = ? AND deleted = false ORDER BY name";
        return cachedList(rc, new PreparedQuery(sql, new Object[]{authEnterpriseId(rc)}));
    }

    @Override
    public CompletableFuture<List<Rbac.Role>> getUserRoles(RequestContext rc) {
        String uid = authUserId(rc);
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
        return query(rc, new PreparedQuery(sql, new Object[]{uid, uid, authEnterpriseId(rc)}));
    }
}
