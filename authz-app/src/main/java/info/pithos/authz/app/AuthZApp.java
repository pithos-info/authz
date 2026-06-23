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

import info.pithos.authz.app.config.AuthZBootstrapConfig;
import info.pithos.authz.app.config.AuthZConfigLoader;
import info.pithos.authz.app.server.AuthZGrpcServer;
import info.pithos.authz.app.server.AuthZHttpServer;
import info.pithos.runtime.core.context.ApplicationContext;
import info.pithos.runtime.core.context.ApplicationContextImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

public final class AuthZApp {

    private static final Logger log = LoggerFactory.getLogger(AuthZApp.class);
    private static final long   LIFECYCLE_TIMEOUT_SECONDS = 30;

    public static void main(String[] args) {
        String configPath = System.getProperty("authz.config", "authz-config.yaml");
        log.info("Loading config from {}", configPath);

        // Phase 1 — construct all modules and the Guice injector (no I/O)
        AuthZBootstrapConfig bootstrapConfig = AuthZConfigLoader.load(configPath);
        ApplicationContext appContext = new ApplicationContextImpl(new AuthZContextCreator(bootstrapConfig));

        // Phase 2 — open all connections, run schema migrations (blocks until ready)
        appContext.start(LIFECYCLE_TIMEOUT_SECONDS, TimeUnit.SECONDS).join();
        log.info("All infrastructure clients started");

        // Phase 3 — bind server ports and accept traffic
        AuthZGrpcServer grpcServer = appContext.getInjector().getInstance(AuthZGrpcServer.class);
        AuthZHttpServer httpServer = appContext.getInjector().getInstance(AuthZHttpServer.class);

        grpcServer.start();
        httpServer.start();

        log.info("AuthZ service ready — HTTP :{} gRPC :{}",
            bootstrapConfig.httpPort(), bootstrapConfig.grpcPort());

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Shutting down AuthZ service");
            httpServer.stop();
            grpcServer.stop();
            // closes all client connections then shuts down executor pools
            appContext.shutdown(LIFECYCLE_TIMEOUT_SECONDS, TimeUnit.SECONDS).join();
        }, "authz-shutdown"));
    }

    private AuthZApp() {}
}
