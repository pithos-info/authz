package info.pithos.rbac.impl;

import info.pithos.data.cache.DistributedCacheClient;
import info.pithos.data.relational.FilterCriteria;
import info.pithos.data.relational.PreparedQuery;
import info.pithos.rbac.GroupService;
import info.pithos.data.relational.client.RelationalClient;
import info.pithos.data.relational.client.ProtoBufCrudService;
import info.pithos.rbac.model.Rbac;
import info.pithos.runtime.core.context.AsyncTaskQueue;
import info.pithos.runtime.model.protocol.Context.RequestContext;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class RelationalGroupService extends ProtoBufCrudService<Rbac.Group> implements GroupService {

    public RelationalGroupService(RelationalClient relationalClient,
                                   DistributedCacheClient cacheClient,
                                   AsyncTaskQueue taskQueue) {
        super(relationalClient, cacheClient, taskQueue, Rbac.Group.getDefaultInstance(), "deleted");
    }

    @Override
    public CompletableFuture<List<Rbac.Group>> list(RequestContext rc) {
        return cachedList(rc, FilterCriteria.eq("enterpriseId", authEnterpriseId(rc)).orderBy("name"));
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
