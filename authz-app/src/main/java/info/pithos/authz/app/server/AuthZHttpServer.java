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

package info.pithos.authz.app.server;

import com.google.inject.Inject;
import info.pithos.authz.app.config.AuthZServerConfig;
import info.pithos.authz.app.rest.AuthZRestRouter;
import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class AuthZHttpServer {

    private static final Logger log = LoggerFactory.getLogger(AuthZHttpServer.class);

    private final AuthZServerConfig config;
    private final AuthZRestRouter restRouter;
    private final Vertx vertx;

    @Inject
    public AuthZHttpServer(AuthZServerConfig config, AuthZRestRouter restRouter) {
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
