package info.pithos.rbac.mcp;

import com.google.inject.Singleton;
import info.pithos.rbac.mcp.auth.McpAuthenticator;
import info.pithos.rbac.mcp.resources.UserContextResource;
import info.pithos.rbac.mcp.tools.AccessManagementTools;
import info.pithos.rbac.mcp.tools.AuthzTools;
import info.pithos.rbac.mcp.tools.IdentityTools;
import info.pithos.runtime.core.context.ApplicationContext;
import info.pithos.runtime.core.context.ServiceModule;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Guice module for the RBAC MCP server.
 *
 * Installed alongside the existing DB module (PostgresRbacModule or CloudSqlRbacModule)
 * in the application context. The MCP server binds to the same service instances as
 * the REST and gRPC layers — no additional service construction needed.
 */
public class RbacMcpModule extends ServiceModule {

    public RbacMcpModule(ApplicationContext context) {
        super(context);
    }

    @Override
    public boolean init() {
        this.initialized.compareAndSet(false, true);
        return this.initialized.get();
    }

    @Override
    protected void configure() {
        super.configure();
        bind(McpAuthenticator.class).in(Singleton.class);
        bind(AuthzTools.class).in(Singleton.class);
        bind(IdentityTools.class).in(Singleton.class);
        bind(AccessManagementTools.class).in(Singleton.class);
        bind(UserContextResource.class).in(Singleton.class);
        bind(RbacMcpServer.class).in(Singleton.class);
    }

    @Override
    public CompletableFuture<Boolean> start(long timeout, TimeUnit unit) {
        // TODO: delegate to RbacMcpServer.start() once transport is wired
        return CompletableFuture.completedFuture(true);
    }

    @Override
    public CompletableFuture<Boolean> shutdown(long timeout, TimeUnit unit) {
        // TODO: delegate to RbacMcpServer.shutdown() once transport is wired
        return CompletableFuture.completedFuture(true);
    }
}
