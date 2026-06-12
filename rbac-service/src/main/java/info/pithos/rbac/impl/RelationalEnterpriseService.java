package info.pithos.rbac.impl;

import info.pithos.data.cache.DistributedCacheClient;
import info.pithos.data.relational.FilterCriteria;
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
    public CompletableFuture<List<Rbac.Enterprise>> list(RequestContext rc) {
        return cachedList(rc, FilterCriteria.none().orderBy("utcCreatedAt"));
    }
}
