package info.pithos.rbac.app.rest.resource;

import com.google.inject.Inject;
import info.pithos.auth.model.Auth.LoginRequest;
import info.pithos.service.container.core.BaseServiceHandler;
import info.pithos.service.container.core.LoginHandler;
import io.vertx.ext.web.Router;

public final class AuthResource {

    private final LoginHandler loginHandler;

    @Inject
    public AuthResource(LoginHandler loginHandler) {
        this.loginHandler = loginHandler;
    }

    public void mount(Router r) {
        r.post("/auth/login").handler(ctx -> {
            LoginRequest req = BaseServiceHandler.parseBody(ctx, LoginRequest.newBuilder());
            if (req == null) return;
            BaseServiceHandler.route(ctx, 200, loginHandler, req);
        });
    }
}
