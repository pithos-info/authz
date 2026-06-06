package info.pithos.rbac.cloudsql;

import info.pithos.data.relational.client.RelationalClient;
import info.pithos.data.relational.cloudsql.CloudSqlClient;
import info.pithos.rbac.RbacServiceModule;
import info.pithos.rbac.impl.RelationalApiKeyService;
import info.pithos.rbac.impl.RelationalEnterpriseService;
import info.pithos.rbac.impl.RelationalGroupService;
import info.pithos.rbac.impl.RelationalRbacService;
import info.pithos.rbac.impl.RelationalRoleService;
import info.pithos.rbac.impl.RelationalUserService;
import info.pithos.runtime.core.context.ApplicationContext;

public final class CloudSqlRbacModule extends RbacServiceModule {

    private CloudSqlClient relationalClient;

    public CloudSqlRbacModule(ApplicationContext context) {
        super(context);
    }

    @Override
    public boolean init() {
        if (this.initialized.compareAndSet(false, true)) {
            this.relationalClient = new CloudSqlClient(this.getApplicationContext());
            this.service          = new RelationalRbacService(this.relationalClient, "db/changelog/cloudsql/db.changelog-master.xml");
            this.enterpriseService = new RelationalEnterpriseService(this.relationalClient);
            this.userService      = new RelationalUserService(this.relationalClient);
            this.groupService     = new RelationalGroupService(this.relationalClient);
            this.roleService      = new RelationalRoleService(this.relationalClient);
            this.apiKeyService    = new RelationalApiKeyService(this.relationalClient);
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
