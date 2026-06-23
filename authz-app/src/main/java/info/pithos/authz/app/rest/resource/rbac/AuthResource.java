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

package info.pithos.authz.app.rest.resource.rbac;

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
