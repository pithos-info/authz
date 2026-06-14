package info.pithos.rbac.app;

import info.pithos.rbac.app.config.RbacBootstrapConfig;
import info.pithos.rbac.app.config.RbacConfigLoader;
import info.pithos.rbac.app.server.RbacGrpcServer;
import info.pithos.rbac.app.server.RbacHttpServer;
import info.pithos.runtime.core.context.ApplicationContext;
import info.pithos.runtime.core.context.ApplicationContextImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

public final class RbacApp {

    private static final Logger log = LoggerFactory.getLogger(RbacApp.class);
    private static final long   LIFECYCLE_TIMEOUT_SECONDS = 30;

    public static void main(String[] args) {
        String configPath = System.getProperty("rbac.config", "rbac-config.yaml");
        log.info("Loading config from {}", configPath);

        // Phase 1 — construct all modules and the Guice injector (no I/O)
        RbacBootstrapConfig bootstrapConfig = RbacConfigLoader.load(configPath);
        ApplicationContext appContext = new ApplicationContextImpl(new RbacContextCreator(bootstrapConfig));

        // Phase 2 — open all connections, run schema migrations (blocks until ready)
        appContext.start(LIFECYCLE_TIMEOUT_SECONDS, TimeUnit.SECONDS).join();
        log.info("All infrastructure clients started");

        // Phase 3 — bind server ports and accept traffic
        RbacGrpcServer grpcServer = appContext.getInjector().getInstance(RbacGrpcServer.class);
        RbacHttpServer httpServer = appContext.getInjector().getInstance(RbacHttpServer.class);

        grpcServer.start();
        httpServer.start();

        log.info("RBAC service ready — HTTP :{} gRPC :{}",
            bootstrapConfig.httpPort(), bootstrapConfig.grpcPort());

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Shutting down RBAC service");
            httpServer.stop();
            grpcServer.stop();
            // closes all client connections then shuts down executor pools
            appContext.shutdown(LIFECYCLE_TIMEOUT_SECONDS, TimeUnit.SECONDS).join();
        }, "rbac-shutdown"));
    }

    private RbacApp() {}
}
