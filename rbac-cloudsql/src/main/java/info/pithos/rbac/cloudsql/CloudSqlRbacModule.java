package info.pithos.rbac.cloudsql;

import info.pithos.data.cache.DistributedCacheClient;
import info.pithos.data.cache.memorystore.MemoryStoreCacheClient;
import info.pithos.data.relational.client.RelationalClient;
import info.pithos.data.relational.cloudsql.CloudSqlClient;
import info.pithos.rbac.RbacServiceModule;
import info.pithos.rbac.impl.RelationalApiKeyService;
import info.pithos.rbac.impl.RelationalEnterpriseService;
import info.pithos.rbac.impl.RelationalGroupMemberService;
import info.pithos.rbac.impl.RelationalGroupRoleService;
import info.pithos.rbac.impl.RelationalGroupService;
import info.pithos.rbac.impl.RelationalRolePermissionService;
import info.pithos.rbac.impl.RelationalRoleService;
import info.pithos.rbac.impl.RelationalUserRoleService;
import info.pithos.rbac.impl.RelationalUserService;
import info.pithos.runtime.core.context.ApplicationContext;
import info.pithos.runtime.core.context.AsyncTaskQueue;
import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.CompletableFuture;

public final class CloudSqlRbacModule extends RbacServiceModule {

    private static final String CHANGELOG = "db/changelog/cloudsql/db.changelog-master.xml";

    private CloudSqlClient relationalClient;
    private MemoryStoreCacheClient cacheClient;

    public CloudSqlRbacModule(ApplicationContext context) {
        super(context);
    }

    @Override
    public boolean init() {
        if (this.initialized.compareAndSet(false, true)) {
            this.relationalClient = new CloudSqlClient(this.getApplicationContext());
            this.cacheClient      = new MemoryStoreCacheClient(this.getApplicationContext());
            AsyncTaskQueue taskQueue = this.getApplicationContext().getSystemContext().getTaskQueue();

            this.userRoleService        = new RelationalUserRoleService(this.relationalClient);
            this.enterpriseService      = new RelationalEnterpriseService(this.relationalClient, this.cacheClient, taskQueue, this.userRoleService);
            this.userService            = new RelationalUserService(this.relationalClient);
            this.groupService           = new RelationalGroupService(this.relationalClient, this.cacheClient, taskQueue);
            this.groupMemberService     = new RelationalGroupMemberService(this.relationalClient);
            this.groupRoleService       = new RelationalGroupRoleService(this.relationalClient);
            this.rolePermissionService  = new RelationalRolePermissionService(this.relationalClient);
            this.roleService            = new RelationalRoleService(this.relationalClient, this.cacheClient, taskQueue);
            this.apiKeyService          = new RelationalApiKeyService(this.relationalClient);
        }
        return this.initialized.get();
    }

    @Override
    public CompletableFuture<Boolean> start(long timeout, TimeUnit unit) {
        return relationalClient.start(timeout, unit)
            .thenCompose(ok -> relationalClient.transaction(conn -> {
                Database db = DatabaseFactory.getInstance()
                    .findCorrectDatabaseImplementation(new JdbcConnection(conn));
                new Liquibase(CHANGELOG, new ClassLoaderResourceAccessor(), db)
                    .update(new Contexts(), new LabelExpression());
            }))
            .thenCompose(v -> cacheClient.start(timeout, unit));
    }

    @Override
    public CompletableFuture<Boolean> shutdown(long timeout, TimeUnit unit) {
        return cacheClient.shutdown(timeout, unit)
            .thenCompose(ok -> relationalClient.shutdown(timeout, unit));
    }

    @Override
    protected void configure() {
        super.configure();
        super.bind(RelationalClient.class).toInstance(this.relationalClient);
        super.bind(CloudSqlClient.class).toInstance(this.relationalClient);
        super.bind(DistributedCacheClient.class).toInstance(this.cacheClient);
        super.bind(MemoryStoreCacheClient.class).toInstance(this.cacheClient);
    }
}
