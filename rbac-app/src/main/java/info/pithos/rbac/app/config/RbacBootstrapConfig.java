package info.pithos.rbac.app.config;

import info.pithos.runtime.model.config.Config.ConfigMap;

public record RbacBootstrapConfig(ConfigMap configMap, int httpPort, int grpcPort, boolean bypassAuth) {}
