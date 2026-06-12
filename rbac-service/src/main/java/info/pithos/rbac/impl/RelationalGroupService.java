package info.pithos.rbac.impl;

import info.pithos.data.cache.DistributedCacheClient;
import info.pithos.data.relational.PreparedQuery;
import info.pithos.rbac.GroupService;
import info.pithos.data.relational.client.RelationalClient;
import info.pithos.data.relational.client.ProtoBufCrudService;
import info.pithos.rbac.model.Rbac;
import info.pithos.runtime.core.context.AsyncTaskQueue;
import info.pithos.runtime.model.protocol.Context.RequestContext;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class RelationalGroupService extends ProtoBufCrudService<Rbac.Group> implements GroupService {

    public RelationalGroupService(RelationalClient relationalClient,
                                   DistributedCacheClient cacheClient,
                                   AsyncTaskQueue taskQueue) {
        super(relationalClient, cacheClient, taskQueue, Rbac.Group.getDefaultInstance(), "deleted");
    }

    @Override
    public CompletableFuture<Rbac.Group> create(RequestContext rc, String name) {
        Rbac.Group group = Rbac.Group.newBuilder()
            .setId(UUID.randomUUID().toString())
            .setEnterpriseId(authEnterpriseId(rc))
            .setName(name)
            .build();
        return save(rc, group);
    }

    @Override
    public CompletableFuture<Rbac.Group> update(RequestContext rc, Rbac.Group group) {
        return merge(rc, group);
    }

    @Override
    public CompletableFuture<Void> delete(RequestContext rc, String id) {
        return remove(rc, id);
    }

    @Override
    public CompletableFuture<List<Rbac.Group>> list(RequestContext rc) {
        String sql = "SELECT " + store.statement().columnList()
            + " FROM \"group\" WHERE \"enterpriseId\" = ? AND deleted = false ORDER BY name";
        return cachedList(rc, new PreparedQuery(sql, new Object[]{authEnterpriseId(rc)}));
    }

    @Override
    public CompletableFuture<List<Rbac.Group>> getUserGroups(RequestContext rc) {
        String sql = "SELECT " + store.statement().columnList()
            + " FROM \"group\""
            + " WHERE id IN (SELECT \"groupId\" FROM \"groupMember\" WHERE \"userId\" = ?)"
            + " AND \"enterpriseId\" = ? AND deleted = false"
            + " ORDER BY name";
        return query(rc, new PreparedQuery(sql, new Object[]{authUserId(rc), authEnterpriseId(rc)}));
    }
}
