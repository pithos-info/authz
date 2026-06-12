package info.pithos.rbac.impl;

import info.pithos.data.cache.DistributedCacheClient;
import info.pithos.data.relational.PreparedQuery;
import info.pithos.rbac.EnterpriseService;
import info.pithos.data.relational.client.RelationalClient;
import info.pithos.data.relational.client.ProtoBufCrudService;
import info.pithos.rbac.model.Rbac;
import info.pithos.runtime.core.context.AsyncTaskQueue;
import info.pithos.runtime.model.protocol.Context.RequestContext;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class RelationalEnterpriseService extends ProtoBufCrudService<Rbac.Enterprise>
        implements EnterpriseService {

    public RelationalEnterpriseService(RelationalClient relationalClient,
                                       DistributedCacheClient cacheClient,
                                       AsyncTaskQueue taskQueue) {
        super(relationalClient, cacheClient, taskQueue, Rbac.Enterprise.getDefaultInstance(), "deleted");
    }

    @Override
    public CompletableFuture<Rbac.Enterprise> create(RequestContext rc, Rbac.Enterprise enterprise) {
        return save(rc, enterprise);
    }

    @Override
    public CompletableFuture<Rbac.Enterprise> update(RequestContext rc, Rbac.Enterprise enterprise) {
        return merge(rc, enterprise);
    }

    @Override
    public CompletableFuture<Void> delete(RequestContext rc, String id) {
        return remove(rc, id);
    }

    @Override
    public CompletableFuture<List<Rbac.Enterprise>> list(RequestContext rc) {
        String sql = "SELECT " + store.statement().columnList()
            + " FROM \"enterprise\" WHERE deleted = false ORDER BY \"utcCreatedAt\"";
        return cachedList(rc, new PreparedQuery(sql, new Object[0]));
    }
}
