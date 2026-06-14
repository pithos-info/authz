package info.pithos.rbac.app.server;

import com.google.inject.Inject;
import info.pithos.rbac.app.config.RbacServerConfig;
import info.pithos.rbac.app.rest.RbacRestRouter;
import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class RbacHttpServer {

    private static final Logger log = LoggerFactory.getLogger(RbacHttpServer.class);

    private final RbacServerConfig config;
    private final RbacRestRouter restRouter;
    private final Vertx vertx;

    @Inject
    public RbacHttpServer(RbacServerConfig config, RbacRestRouter restRouter) {
        this.config = config;
        this.restRouter = restRouter;
        this.vertx = Vertx.vertx();
    }

    public void start() {
        Router router = Router.router(vertx);
        router.route().handler(BodyHandler.create());
        restRouter.mount(router);

        vertx.createHttpServer()
            .requestHandler(router)
            .listen(config.httpPort())
            .toCompletionStage()
            .toCompletableFuture()
            .join();

        log.info("HTTP server listening on :{}", config.httpPort());
    }

    public void stop() {
        vertx.close().toCompletionStage().toCompletableFuture().join();
    }
}
