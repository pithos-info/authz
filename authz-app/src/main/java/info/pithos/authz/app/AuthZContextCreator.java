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

package info.pithos.authz.app;

import info.pithos.authn.gcp.GcpIdentityOAuthModule;
import info.pithos.data.blob.minio.MinioBlobStorageModule;
import info.pithos.authz.app.auth.BypassAuthModule;
import info.pithos.authz.app.config.AuthZBootstrapConfig;
import info.pithos.monetization.postgres.PostgresMonetizationModule;
import info.pithos.rbac.postgres.PostgresRbacModule;
import info.pithos.runtime.core.context.ApplicationContext;
import info.pithos.runtime.core.context.ContextCreator;
import info.pithos.runtime.core.context.ServiceModule;
import info.pithos.runtime.model.config.Config.ConfigMap;
import info.pithos.vault.hashicorp.HashiCorpVaultModule;

import java.util.List;

final class AuthZContextCreator implements ContextCreator {

    private final AuthZBootstrapConfig bootstrapConfig;

    AuthZContextCreator(AuthZBootstrapConfig bootstrapConfig) {
        this.bootstrapConfig = bootstrapConfig;
    }

    @Override
    public ConfigMap getConfigMap() {
        return bootstrapConfig.configMap();
    }

    @Override
    public Iterable<ServiceModule> getInjectionModules(ApplicationContext context) {
        ServiceModule authModule = bootstrapConfig.bypassAuth()
            ? new BypassAuthModule(context)
            : new GcpIdentityOAuthModule(context);
        return List.of(
            new PostgresRbacModule(context),
            new PostgresMonetizationModule(context),
            authModule,
            new HashiCorpVaultModule(context),
            new MinioBlobStorageModule(context),
            new AuthZAppModule(context, bootstrapConfig.httpPort(), bootstrapConfig.grpcPort(), bootstrapConfig.bypassAuth())
        );
    }
}
