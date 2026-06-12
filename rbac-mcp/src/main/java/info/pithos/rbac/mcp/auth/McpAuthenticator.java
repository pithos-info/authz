package info.pithos.rbac.mcp.auth;

import com.google.inject.Inject;
import info.pithos.auth.OAuthClient;
import info.pithos.runtime.model.protocol.Context.RequestContext;
import info.pithos.service.container.core.auth.ApiKeyResolver;

import java.util.concurrent.CompletableFuture;

/**
 * Resolves an MCP session's credentials to a {@link RequestContext}.
 *
 * MCP connections are long-lived (SSE), so auth resolves once at connection time — not
 * per tool call. The resulting RequestContext is then scoped to the session and passed
 * to every tool and resource call within it.
 *
 * Two auth paths, mirroring the REST layer:
 * <ul>
 *   <li>API key (headless agents / service accounts) — Bearer token without dots</li>
 *   <li>OAuth JWT (user-delegated agents) — Bearer token with dots</li>
 * </ul>
 *
 * TODO: called from the MCP server's SSE connection handler once transport is wired.
 */
public class McpAuthenticator {

    private final OAuthClient     oAuthClient;
    private final ApiKeyResolver  apiKeyResolver;

    @Inject
    public McpAuthenticator(OAuthClient oAuthClient, ApiKeyResolver apiKeyResolver) {
        this.oAuthClient    = oAuthClient;
        this.apiKeyResolver = apiKeyResolver;
    }

    /**
     * Authenticates the MCP session and returns a RequestContext for it.
     *
     * @param bearerToken  raw value from {@code Authorization: Bearer <token>}
     * @param enterpriseId from {@code X-Enterprise-Id} header (required for OAuth path)
     */
    public CompletableFuture<RequestContext> authenticate(String bearerToken, String enterpriseId) {
        // TODO: implement — mirror BaseServiceHandler.handleHttp auth logic:
        //   isApiKey(bearerToken) → apiKeyResolver.resolve(bootstrap, bearerToken)
        //                           → build RC with userId + enterpriseId from key record
        //   else                  → oAuthClient.introspectToken(bearerToken)
        //                           → verify active, build RC with subject + enterpriseId header
        throw new UnsupportedOperationException("TODO: implement MCP session auth");
    }

    private static boolean isApiKey(String token) {
        return token != null && !token.contains(".");
    }
}
