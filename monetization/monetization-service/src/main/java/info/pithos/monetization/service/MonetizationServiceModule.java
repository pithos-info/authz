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

package info.pithos.monetization.service;

import info.pithos.monetization.service.relational.RelationalAppService;
import info.pithos.monetization.service.relational.RelationalFeatureService;
import info.pithos.monetization.service.relational.RelationalJourneyService;
import info.pithos.monetization.service.relational.RelationalWorkflowFeatureService;
import info.pithos.monetization.service.relational.RelationalWorkflowService;
import info.pithos.runtime.core.context.ApplicationContext;
import info.pithos.runtime.core.context.ServiceModule;

public abstract class MonetizationServiceModule extends ServiceModule {

    protected RelationalAppService             appService;
    protected RelationalFeatureService         featureService;
    protected RelationalJourneyService         journeyService;
    protected RelationalWorkflowService        workflowService;
    protected RelationalWorkflowFeatureService workflowFeatureService;

    protected MonetizationServiceModule(ApplicationContext context) {
        super(context);
    }

    @Override
    protected void configure() {
        super.configure();
        bind(AppService.class).toInstance(this.appService);
        bind(RelationalAppService.class).toInstance(this.appService);
        bind(FeatureService.class).toInstance(this.featureService);
        bind(RelationalFeatureService.class).toInstance(this.featureService);
        bind(JourneyService.class).toInstance(this.journeyService);
        bind(RelationalJourneyService.class).toInstance(this.journeyService);
        bind(WorkflowService.class).toInstance(this.workflowService);
        bind(RelationalWorkflowService.class).toInstance(this.workflowService);
        bind(WorkflowFeatureService.class).toInstance(this.workflowFeatureService);
        bind(RelationalWorkflowFeatureService.class).toInstance(this.workflowFeatureService);
    }
}
