package info.pithos.rbac.app.auth;

import com.google.inject.Inject;
import info.pithos.rbac.EnterpriseService;
import info.pithos.rbac.UserService;
import info.pithos.runtime.core.context.ErrorCode;
import info.pithos.runtime.core.context.ServiceException;
import info.pithos.runtime.model.protocol.Context.AuthContext;
import info.pithos.runtime.model.protocol.Context.RequestContext;
import info.pithos.service.container.core.auth.UserContextResolver;

import java.util.concurrent.CompletableFuture;

public final class RbacUserContextResolver implements UserContextResolver {

    private final EnterpriseService enterpriseService;
    private final UserService userService;

    @Inject
    public RbacUserContextResolver(EnterpriseService enterpriseService, UserService userService) {
        this.enterpriseService = enterpriseService;
        this.userService = userService;
    }

    @Override
    public CompletableFuture<RequestContext> resolve(RequestContext rc) {
        String enterpriseId = rc.getEnterpriseId();
        String externalId = rc.getAuthContext().getUserId();

        if (enterpriseId.isBlank()) {
            return CompletableFuture.failedFuture(
                new ServiceException(ErrorCode.UNAUTHORIZED, "X-Enterprise-Id header required"));
        }

        return enterpriseService.get(rc, enterpriseId)
            .thenCompose(entOpt -> {
                if (entOpt.isEmpty()) {
                    throw new ServiceException(ErrorCode.UNAUTHORIZED,
                        "enterprise not found: " + enterpriseId);
                }
                return userService.findByExternalId(rc, externalId);
            })
            .thenApply(userOpt -> {
                if (userOpt.isEmpty()) {
                    throw new ServiceException(ErrorCode.UNAUTHORIZED,
                        "user not found in enterprise: " + externalId);
                }
                // Replace the IdP sub with the RBAC user ID so all downstream
                // role/permission checks operate on the internal user identity.
                String rbacUserId = userOpt.get().getId();
                AuthContext enriched = rc.getAuthContext().toBuilder()
                    .setUserId(rbacUserId)
                    .build();
                return rc.toBuilder().setAuthContext(enriched).build();
            });
    }
}
