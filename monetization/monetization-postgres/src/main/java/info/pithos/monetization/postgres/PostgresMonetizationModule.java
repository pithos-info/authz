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

package info.pithos.monetization.postgres;

import info.pithos.data.cache.DistributedCacheClient;
import info.pithos.data.cache.redis.RedisCacheClient;
import info.pithos.data.relational.client.RelationalClient;
import info.pithos.data.relational.postgres.PostgresClient;
import info.pithos.monetization.service.MonetizationServiceModule;
import info.pithos.monetization.service.relational.RelationalAppService;
import info.pithos.monetization.service.relational.RelationalFeatureService;
import info.pithos.monetization.service.relational.RelationalJourneyService;
import info.pithos.monetization.service.relational.RelationalWorkflowFeatureService;
import info.pithos.monetization.service.relational.RelationalWorkflowService;
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

public final class PostgresMonetizationModule extends MonetizationServiceModule {

    private static final String CHANGELOG = "db/changelog/postgres/db.changelog-master.xml";

    private PostgresClient   relationalClient;
    private RedisCacheClient cacheClient;

    public PostgresMonetizationModule(ApplicationContext context) {
        super(context);
    }

    @Override
    public boolean init() {
        if (this.initialized.compareAndSet(false, true)) {
            AsyncTaskQueue taskQueue        = this.getApplicationContext().getSystemContext().getTaskQueue();
            this.relationalClient           = new PostgresClient(this.getApplicationContext());
            this.cacheClient                = new RedisCacheClient(this.getApplicationContext());
            this.appService                 = new RelationalAppService(this.relationalClient, this.cacheClient, taskQueue);
            this.featureService             = new RelationalFeatureService(this.relationalClient, this.cacheClient, taskQueue);
            this.journeyService             = new RelationalJourneyService(this.relationalClient, this.cacheClient, taskQueue);
            this.workflowService            = new RelationalWorkflowService(this.relationalClient, this.cacheClient, taskQueue);
            this.workflowFeatureService     = new RelationalWorkflowFeatureService(this.relationalClient, this.cacheClient, taskQueue);
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
        bind(RelationalClient.class).toInstance(this.relationalClient);
        bind(PostgresClient.class).toInstance(this.relationalClient);
        bind(DistributedCacheClient.class).toInstance(this.cacheClient);
        bind(RedisCacheClient.class).toInstance(this.cacheClient);
    }
}
