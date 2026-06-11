package info.pithos.rbac.impl;

import info.pithos.data.cache.DistributedCacheClient;
import info.pithos.data.cache.ProtoBufListCache;
import info.pithos.data.relational.PreparedQuery;
import info.pithos.data.relational.client.ProtoBufRelationalClient;
import info.pithos.data.relational.client.RelationalClient;
import info.pithos.rbac.AbstractRbacService;
import info.pithos.rbac.GroupService;
import info.pithos.rbac.model.Rbac;
import info.pithos.runtime.core.context.AsyncTaskQueue;
import info.pithos.runtime.model.protocol.Context.RequestContext;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class RelationalGroupService extends AbstractRbacService implements GroupService {

    private final ProtoBufRelationalClient<Rbac.Group> store;
    private final ProtoBufListCache<Rbac.Group> listCache;
    private final AsyncTaskQueue taskQueue;

    public RelationalGroupService(RelationalClient relationalClient,
                                   DistributedCacheClient cacheClient,
                                   AsyncTaskQueue taskQueue) {
        super(relationalClient);
        this.store = ProtoBufRelationalClient.of(relationalClient, cacheClient, taskQueue,
                                                 Rbac.Group.getDefaultInstance(), "deleted");
        this.listCache = ProtoBufListCache.of(cacheClient, Rbac.Group.getDefaultInstance());
        this.taskQueue = taskQueue;
    }

    @Override
    public CompletableFuture<Rbac.Group> create(RequestContext rc, String name) {
        Rbac.Group group = Rbac.Group.newBuilder()
            .setId(UUID.randomUUID().toString())
            .setEnterpriseId(authEnterpriseId(rc))
            .setName(name)
            .build();
        return store.insert(dc(rc), group)
            .thenCompose(created -> listCache.delete(rc, listCache.listKey())
                .thenApply(d -> created));
    }

    @Override
    public CompletableFuture<Optional<Rbac.Group>> get(RequestContext rc, String id) {
        return store.findById(dc(rc), id).thenApply(Optional::ofNullable);
    }

    @Override
    public CompletableFuture<Rbac.Group> update(RequestContext rc, Rbac.Group group) {
        return store.update(dc(rc), group)
            .thenCompose(updated -> listCache.delete(rc, listCache.listKey())
                .thenApply(d -> updated));
    }

    @Override
    public CompletableFuture<Void> delete(RequestContext rc, String id) {
        return store.softDelete(dc(rc), id)
            .thenCompose(n -> listCache.delete(rc, listCache.listKey()))
            .thenAccept(d -> {});
    }

    @Override
    public CompletableFuture<List<Rbac.Group>> list(RequestContext rc) {
        return listCache.get(rc, listCache.listKey()).thenCompose(cached -> {
            if (cached != null) return CompletableFuture.completedFuture(cached);
            String sql = "SELECT " + store.statement().columnList()
                + " FROM \"group\" WHERE \"enterpriseId\" = ? AND deleted = false ORDER BY name";
            return store.findAll(dc(rc), new PreparedQuery(sql, new Object[]{authEnterpriseId(rc)}))
                .thenApply(groups -> {
                    taskQueue.enqueue(() -> listCache.set(rc, listCache.listKey(), groups));
                    return groups;
                });
        });
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
