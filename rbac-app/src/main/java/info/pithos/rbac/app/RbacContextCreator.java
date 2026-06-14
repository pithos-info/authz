package info.pithos.rbac.app;

import info.pithos.auth.gcp.GcpIdentityOAuthModule;
import info.pithos.data.blob.minio.MinioBlobStorageModule;
import info.pithos.rbac.app.auth.BypassAuthModule;
import info.pithos.rbac.app.config.RbacBootstrapConfig;
import info.pithos.rbac.postgres.PostgresRbacModule;
import info.pithos.runtime.core.context.ApplicationContext;
import info.pithos.runtime.core.context.ContextCreator;
import info.pithos.runtime.core.context.ServiceModule;
import info.pithos.runtime.model.config.Config.ConfigMap;
import info.pithos.vault.hashicorp.HashiCorpVaultModule;

import java.util.List;

final class RbacContextCreator implements ContextCreator {

    private final RbacBootstrapConfig bootstrapConfig;

    RbacContextCreator(RbacBootstrapConfig bootstrapConfig) {
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
            authModule,
            new HashiCorpVaultModule(context),
            new MinioBlobStorageModule(context),
            new RbacAppModule(context, bootstrapConfig.httpPort(), bootstrapConfig.grpcPort(), bootstrapConfig.bypassAuth())
        );
    }
}
