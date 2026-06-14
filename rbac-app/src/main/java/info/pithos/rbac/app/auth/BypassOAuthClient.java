package info.pithos.rbac.app.auth;

import info.pithos.auth.OAuthClient;
import info.pithos.auth.model.TokenIntrospection;
import info.pithos.auth.model.TokenResponse;
import info.pithos.auth.model.TokenType;
import info.pithos.auth.model.UserInfo;
import info.pithos.runtime.model.protocol.Context.RequestContext;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public final class BypassOAuthClient implements OAuthClient {

    @Override
    public CompletableFuture<TokenIntrospection> introspectToken(RequestContext rc, String token) {
        String userId = rc.getAuthContext().getUserId();
        String enterpriseId = rc.getEnterpriseId();
        return CompletableFuture.completedFuture(
            new TokenIntrospection(true, userId, enterpriseId, null, null, 0L, 0L, null, List.of())
        );
    }

    @Override
    public CompletableFuture<TokenResponse> clientCredentialsGrant(RequestContext rc, List<String> scopes) {
        throw new UnsupportedOperationException("bypass auth mode");
    }

    @Override
    public CompletableFuture<TokenResponse> login(RequestContext rc, String username, String password) {
        throw new UnsupportedOperationException("bypass auth mode");
    }

    @Override
    public CompletableFuture<TokenResponse> loginWithIdToken(RequestContext rc, String idToken) {
        throw new UnsupportedOperationException("bypass auth mode");
    }

    @Override
    public CompletableFuture<TokenResponse> refreshToken(RequestContext rc, String refreshToken) {
        throw new UnsupportedOperationException("bypass auth mode");
    }

    @Override
    public CompletableFuture<Void> revokeToken(RequestContext rc, String token, TokenType tokenType) {
        throw new UnsupportedOperationException("bypass auth mode");
    }

    @Override
    public CompletableFuture<UserInfo> getUserInfo(RequestContext rc, String accessToken) {
        throw new UnsupportedOperationException("bypass auth mode");
    }

    @Override
    public CompletableFuture<Boolean> start(long timeout, TimeUnit unit) {
        return CompletableFuture.completedFuture(true);
    }

    @Override
    public CompletableFuture<Boolean> shutdown(long timeout, TimeUnit unit) {
        return CompletableFuture.completedFuture(true);
    }
}
