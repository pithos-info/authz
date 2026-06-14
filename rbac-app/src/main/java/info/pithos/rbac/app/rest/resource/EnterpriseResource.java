package info.pithos.rbac.app.rest.resource;

import com.google.inject.Inject;
import info.pithos.rbac.app.handler.EnterpriseHandlers;
import info.pithos.rbac.service.CreateEnterpriseRequest;
import info.pithos.rbac.service.DeleteByIdRequest;
import info.pithos.rbac.service.Empty;
import info.pithos.rbac.service.GetByIdRequest;
import info.pithos.rbac.service.UpdateEnterpriseRequest;
import info.pithos.service.container.core.BaseServiceHandler;
import io.vertx.ext.web.Router;

public final class EnterpriseResource {

    private final EnterpriseHandlers.Create create;
    private final EnterpriseHandlers.Get    get;
    private final EnterpriseHandlers.Update update;
    private final EnterpriseHandlers.Delete delete;
    private final EnterpriseHandlers.List   list;

    @Inject
    public EnterpriseResource(
            EnterpriseHandlers.Create create,
            EnterpriseHandlers.Get    get,
            EnterpriseHandlers.Update update,
            EnterpriseHandlers.Delete delete,
            EnterpriseHandlers.List   list) {
        this.create = create;
        this.get    = get;
        this.update = update;
        this.delete = delete;
        this.list   = list;
    }

    public void mount(Router r) {
        r.get("/enterprises").handler(ctx ->
            BaseServiceHandler.route(ctx, 200, list, Empty.getDefaultInstance()));

        r.post("/enterprises").handler(ctx -> {
            CreateEnterpriseRequest req = BaseServiceHandler.parseBody(ctx, CreateEnterpriseRequest.newBuilder());
            if (req == null) return;
            BaseServiceHandler.route(ctx, 201, create, req);
        });

        r.get("/enterprises/:id").handler(ctx ->
            BaseServiceHandler.route(ctx, 200, get,
                GetByIdRequest.newBuilder().setId(ctx.pathParam("id")).build()));

        r.put("/enterprises/:id").handler(ctx -> {
            UpdateEnterpriseRequest req = BaseServiceHandler.parseBody(ctx, UpdateEnterpriseRequest.newBuilder());
            if (req == null) return;
            BaseServiceHandler.route(ctx, 200, update,
                req.toBuilder().setId(ctx.pathParam("id")).build());
        });

        r.delete("/enterprises/:id").handler(ctx ->
            BaseServiceHandler.routeNoContent(ctx, delete,
                DeleteByIdRequest.newBuilder().setId(ctx.pathParam("id")).build()));
    }
}
