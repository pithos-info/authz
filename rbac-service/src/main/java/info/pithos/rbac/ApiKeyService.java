package info.pithos.rbac;

import info.pithos.rbac.model.Rbac;
import info.pithos.runtime.model.protocol.http.Context.RequestContext;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface ApiKeyService {

    /** Creates an API key; {@code apiKey.enterpriseId} and {@code apiKey.userId} must be set. */
    CompletableFuture<Rbac.ApiKey> create(RequestContext rc, Rbac.ApiKey apiKey);

    CompletableFuture<Optional<Rbac.ApiKey>> get(RequestContext rc, String id);

    CompletableFuture<Void> revoke(RequestContext rc, String id);

    /** Lists API keys for {@code authContext.userId} within {@code authContext.enterpriseId}. */
    CompletableFuture<List<Rbac.ApiKey>> list(RequestContext rc);
}
