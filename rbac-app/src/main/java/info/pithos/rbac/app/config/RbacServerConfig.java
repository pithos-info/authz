package info.pithos.rbac.app.config;

public record RbacServerConfig(int httpPort, int grpcPort, boolean bypassAuth) {}
