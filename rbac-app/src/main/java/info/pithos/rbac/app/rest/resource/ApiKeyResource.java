package info.pithos.rbac.app.rest.resource;

import com.google.inject.Inject;
import info.pithos.rbac.app.handler.ApiKeyHandlers;
import info.pithos.rbac.service.CreateApiKeyRequest;
import info.pithos.rbac.service.DeleteByIdRequest;
import info.pithos.rbac.service.Empty;
import info.pithos.rbac.service.GetByIdRequest;
import info.pithos.service.container.core.BaseServiceHandler;
import io.vertx.ext.web.Router;

public final class ApiKeyResource {

    private final ApiKeyHandlers.Create create;
    private final ApiKeyHandlers.Get    get;
    private final ApiKeyHandlers.Revoke revoke;
    private final ApiKeyHandlers.List   list;

    @Inject
    public ApiKeyResource(
            ApiKeyHandlers.Create create,
            ApiKeyHandlers.Get    get,
            ApiKeyHandlers.Revoke revoke,
            ApiKeyHandlers.List   list) {
        this.create = create;
        this.get    = get;
        this.revoke = revoke;
        this.list   = list;
    }

    public void mount(Router r) {
        r.get("/apikeys").handler(ctx ->
            BaseServiceHandler.route(ctx, 200, list, Empty.getDefaultInstance()));

        r.post("/apikeys").handler(ctx -> {
            CreateApiKeyRequest req = BaseServiceHandler.parseBody(ctx, CreateApiKeyRequest.newBuilder());
            if (req == null) return;
            BaseServiceHandler.route(ctx, 201, create, req);
        });

        r.get("/apikeys/:id").handler(ctx ->
            BaseServiceHandler.route(ctx, 200, get,
                GetByIdRequest.newBuilder().setId(ctx.pathParam("id")).build()));

        r.delete("/apikeys/:id").handler(ctx ->
            BaseServiceHandler.routeNoContent(ctx, revoke,
                DeleteByIdRequest.newBuilder().setId(ctx.pathParam("id")).build()));
    }
}
