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

package info.pithos.rbac.mcp;

import com.google.inject.Inject;
import info.pithos.rbac.mcp.auth.McpAuthenticator;
import info.pithos.rbac.mcp.resources.UserContextResource;
import info.pithos.rbac.mcp.tools.AccessManagementTools;
import info.pithos.rbac.mcp.tools.AuthzTools;
import info.pithos.rbac.mcp.tools.IdentityTools;
import info.pithos.runtime.core.context.ServiceLifeCycle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * MCP server lifecycle for the RBAC service.
 *
 * Registers all tools and resources, then starts the transport.
 * Auth resolves at SSE connection time via {@link McpAuthenticator} — the resulting
 * {@code RequestContext} is scoped to the session and passed to every tool call.
 *
 * Transport options (pick one when wiring the real SDK):
 *   - SSE over HTTP: agents connect via GET /sse, post tool calls to POST /message
 *   - stdio: for local agent processes (simpler, no network exposure)
 *
 * TODO: inject McpServer from io.modelcontextprotocol.sdk:mcp, configure transport,
 *       and register tools/resources via the SDK's builder API.
 */
public class RbacMcpServer implements ServiceLifeCycle {

    private static final Logger log = LoggerFactory.getLogger(RbacMcpServer.class);

    private final McpAuthenticator      authenticator;
    private final AuthzTools            authzTools;
    private final IdentityTools         identityTools;
    private final AccessManagementTools accessTools;
    private final UserContextResource   userContextResource;

    @Inject
    public RbacMcpServer(McpAuthenticator authenticator,
                          AuthzTools authzTools,
                          IdentityTools identityTools,
                          AccessManagementTools accessTools,
                          UserContextResource userContextResource) {
        this.authenticator       = authenticator;
        this.authzTools          = authzTools;
        this.identityTools       = identityTools;
        this.accessTools         = accessTools;
        this.userContextResource = userContextResource;
    }

    @Override
    public CompletableFuture<Boolean> start(long timeout, TimeUnit unit) {
        log.info("Starting RBAC MCP server");
        // TODO:
        //   McpServer server = McpServer.sync(transport)
        //       .serverInfo("rbac-mcp", "1.0.0")
        //       .tools(/* register authzTools, identityTools, accessTools */)
        //       .resources(/* register userContextResource */)
        //       .build();
        //   server.start();
        log.warn("RBAC MCP server is stubbed — transport not yet configured");
        return CompletableFuture.completedFuture(true);
    }

    @Override
    public CompletableFuture<Boolean> shutdown(long timeout, TimeUnit unit) {
        log.info("Stopping RBAC MCP server");
        // TODO: server.close();
        return CompletableFuture.completedFuture(true);
    }
}
