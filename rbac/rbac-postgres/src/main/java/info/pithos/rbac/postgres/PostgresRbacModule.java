/*
 * Copyright 2026 Pithos
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package info.pithos.rbac.postgres;

import info.pithos.data.cache.DistributedCacheClient;
import info.pithos.data.cache.redis.RedisCacheClient;
import info.pithos.data.relational.client.RelationalClient;
import info.pithos.data.relational.postgres.PostgresClient;
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

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeUnit;

public final class PostgresRbacModule extends RbacServiceModule {

    private static final String CHANGELOG = "db/changelog/postgres/db.changelog-master.xml";

    private PostgresClient relationalClient;
    private RedisCacheClient cacheClient;

    public PostgresRbacModule(ApplicationContext context) {
        super(context);
    }

    @Override
    public boolean init() {
        if (this.initialized.compareAndSet(false, true)) {
            this.relationalClient = new PostgresClient(this.getApplicationContext());
            this.cacheClient      = new RedisCacheClient(this.getApplicationContext());
            AsyncTaskQueue taskQueue = this.getApplicationContext().getSystemContext().getTaskQueue();

            this.enterpriseService      = new RelationalEnterpriseService(this.relationalClient, this.cacheClient, taskQueue);
            this.userService            = new RelationalUserService(this.relationalClient);
            this.groupService           = new RelationalGroupService(this.relationalClient, this.cacheClient, taskQueue);
            this.groupMemberService     = new RelationalGroupMemberService(this.relationalClient);
            this.userRoleService        = new RelationalUserRoleService(this.relationalClient);
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
        super.bind(PostgresClient.class).toInstance(this.relationalClient);
        super.bind(DistributedCacheClient.class).toInstance(this.cacheClient);
        super.bind(RedisCacheClient.class).toInstance(this.cacheClient);
    }
}
