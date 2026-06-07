package info.pithos.rbac.cloudsql;

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
import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;

public final class CloudSqlRbacModule extends RbacServiceModule {

    private static final String CHANGELOG = "db/changelog/cloudsql/db.changelog-master.xml";

    private CloudSqlClient relationalClient;

    public CloudSqlRbacModule(ApplicationContext context) {
        super(context);
    }

    @Override
    public boolean init() {
        if (this.initialized.compareAndSet(false, true)) {
            this.relationalClient = new CloudSqlClient(this.getApplicationContext());
            this.relationalClient.start(30, java.util.concurrent.TimeUnit.SECONDS)
                .thenCompose(started -> this.relationalClient.transaction(conn -> {
                    Database db = DatabaseFactory.getInstance()
                        .findCorrectDatabaseImplementation(new JdbcConnection(conn));
                    new Liquibase(CHANGELOG, new ClassLoaderResourceAccessor(), db)
                        .update(new Contexts(), new LabelExpression());
                }))
                .join();
            this.enterpriseService      = new RelationalEnterpriseService(this.relationalClient);
            this.userService            = new RelationalUserService(this.relationalClient);
            this.groupService           = new RelationalGroupService(this.relationalClient);
            this.groupMemberService     = new RelationalGroupMemberService(this.relationalClient);
            this.userRoleService        = new RelationalUserRoleService(this.relationalClient);
            this.groupRoleService       = new RelationalGroupRoleService(this.relationalClient);
            this.rolePermissionService  = new RelationalRolePermissionService(this.relationalClient);
            this.roleService            = new RelationalRoleService(this.relationalClient);
            this.apiKeyService          = new RelationalApiKeyService(this.relationalClient);
        }
        return this.initialized.get();
    }

    @Override
    protected void configure() {
        super.configure();
        super.bind(RelationalClient.class).toInstance(this.relationalClient);
        super.bind(CloudSqlClient.class).toInstance(this.relationalClient);
    }
}
