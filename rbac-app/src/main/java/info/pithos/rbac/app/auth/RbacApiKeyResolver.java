package info.pithos.rbac.app.auth;

import com.google.inject.Inject;
import info.pithos.auth.model.TokenIntrospection;
import info.pithos.rbac.ApiKeyService;
import info.pithos.rbac.model.Rbac;
import info.pithos.runtime.model.protocol.Context.RequestContext;
import info.pithos.service.container.core.auth.ApiKeyResolver;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class RbacApiKeyResolver implements ApiKeyResolver {

    private static final TokenIntrospection INACTIVE =
        new TokenIntrospection(false, null, null, null, null, 0L, 0L, null, List.of());

    private final ApiKeyService apiKeyService;

    @Inject
    public RbacApiKeyResolver(ApiKeyService apiKeyService) {
        this.apiKeyService = apiKeyService;
    }

    @Override
    public CompletableFuture<TokenIntrospection> resolve(RequestContext bootstrap, String rawKey) {
        String keyHash = ApiKeyResolver.sha256hex(rawKey);
        return apiKeyService.findByKeyHash(bootstrap, keyHash)
            .thenApply(opt -> opt
                .filter(key -> !isExpired(key))
                .map(key -> {
                    apiKeyService.touch(bootstrap, key.getId());
                    return new TokenIntrospection(
                        true,
                        key.getUserId(),
                        key.getEnterpriseId(),
                        null,
                        null,
                        key.getExpiresAt(),
                        0L,
                        null,
                        List.of()
                    );
                })
                .orElse(INACTIVE)
            );
    }

    private static boolean isExpired(Rbac.ApiKey key) {
        return key.getExpiresAt() > 0 && key.getExpiresAt() < System.currentTimeMillis();
    }
}
