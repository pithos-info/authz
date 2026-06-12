package info.pithos.rbac.impl;

import info.pithos.data.cache.DistributedCacheClient;
import info.pithos.data.relational.PreparedQuery;
import info.pithos.data.relational.client.ProtoBufRelationalClient;
import info.pithos.data.relational.client.RelationalClient;
import info.pithos.rbac.AbstractRbacService;
import info.pithos.rbac.EnterpriseService;
import info.pithos.rbac.UserRoleService;
import info.pithos.rbac.model.Rbac;
import info.pithos.runtime.core.context.AsyncTaskQueue;
import info.pithos.runtime.core.context.ErrorCode;
import info.pithos.runtime.core.context.ServiceException;
import info.pithos.runtime.model.protocol.Context.RequestContext;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class RelationalEnterpriseService extends AbstractRbacService implements EnterpriseService {

    private static final String SYSTEM_ENTERPRISE_ID = "1";
    private static final String ADMIN_ROLE_ID = "1";

    private final ProtoBufRelationalClient<Rbac.Enterprise> store;
    private final UserRoleService userRoleService;

    public RelationalEnterpriseService(RelationalClient relationalClient,
                                       DistributedCacheClient cacheClient,
                                       AsyncTaskQueue taskQueue,
                                       UserRoleService userRoleService) {
        super(relationalClient);
        this.userRoleService = userRoleService;
        this.store = ProtoBufRelationalClient.of(relationalClient, cacheClient, taskQueue,
                                                 Rbac.Enterprise.getDefaultInstance(), "deleted");
    }

    @Override
    public CompletableFuture<Rbac.Enterprise> create(RequestContext rc, Rbac.Enterprise enterprise) {
        return requireSystemAdmin(rc).thenCompose(v -> store.insert(dc(rc), enterprise));
    }

    @Override
    public CompletableFuture<Optional<Rbac.Enterprise>> get(RequestContext rc, String id) {
        return store.findById(dc(rc), id).thenApply(Optional::ofNullable);
    }

    @Override
    public CompletableFuture<Rbac.Enterprise> update(RequestContext rc, Rbac.Enterprise enterprise) {
        return requireSystemAdmin(rc).thenCompose(v -> store.update(dc(rc), enterprise));
    }

    @Override
    public CompletableFuture<Void> delete(RequestContext rc, String id) {
        return requireSystemAdmin(rc).thenCompose(v -> store.softDelete(dc(rc), id).thenAccept(n -> {}));
    }

    @Override
    public CompletableFuture<List<Rbac.Enterprise>> list(RequestContext rc) {
        return requireSystemAdmin(rc).thenCompose(v -> {
            String sql = "SELECT " + store.statement().columnList()
                + " FROM \"enterprise\" WHERE deleted = false ORDER BY \"utcCreatedAt\"";
            return store.findAll(dc(rc), new PreparedQuery(sql, new Object[0]));
        });
    }

    private CompletableFuture<Void> requireSystemAdmin(RequestContext rc) {
        if (!SYSTEM_ENTERPRISE_ID.equals(authEnterpriseId(rc))) {
            return CompletableFuture.failedFuture(
                new ServiceException(ErrorCode.FORBIDDEN, "system enterprise access required"));
        }
        return userRoleService.hasRole(rc, ADMIN_ROLE_ID).thenAccept(isAdmin -> {
            if (!isAdmin) throw new ServiceException(ErrorCode.FORBIDDEN, "admin role required");
        });
    }
}
