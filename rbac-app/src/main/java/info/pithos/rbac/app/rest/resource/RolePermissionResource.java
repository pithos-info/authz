package info.pithos.rbac.app.rest.resource;

import com.google.inject.Inject;
import info.pithos.rbac.app.handler.RolePermissionHandlers;
import info.pithos.rbac.service.AddRolePermissionRequest;
import info.pithos.rbac.service.GetByIdRequest;
import info.pithos.rbac.service.RemoveRolePermissionRequest;
import info.pithos.service.container.core.BaseServiceHandler;
import io.vertx.ext.web.Router;

public final class RolePermissionResource {

    private final RolePermissionHandlers.Add      add;
    private final RolePermissionHandlers.Remove   remove;
    private final RolePermissionHandlers.ListByRole listByRole;

    @Inject
    public RolePermissionResource(
            RolePermissionHandlers.Add      add,
            RolePermissionHandlers.Remove   remove,
            RolePermissionHandlers.ListByRole listByRole) {
        this.add        = add;
        this.remove     = remove;
        this.listByRole = listByRole;
    }

    public void mount(Router r) {
        r.post("/roles/:roleId/permissions").handler(ctx -> {
            AddRolePermissionRequest req = BaseServiceHandler.parseBody(ctx, AddRolePermissionRequest.newBuilder());
            if (req == null) return;
            BaseServiceHandler.route(ctx, 201, add,
                req.toBuilder().setRoleId(ctx.pathParam("roleId")).build());
        });

        r.delete("/roles/:roleId/permissions/:permission").handler(ctx ->
            BaseServiceHandler.routeNoContent(ctx, remove,
                RemoveRolePermissionRequest.newBuilder()
                    .setRoleId(ctx.pathParam("roleId"))
                    .setPermission(ctx.pathParam("permission"))
                    .build()));

        r.get("/roles/:roleId/permissions").handler(ctx ->
            BaseServiceHandler.route(ctx, 200, listByRole,
                GetByIdRequest.newBuilder().setId(ctx.pathParam("roleId")).build()));
    }
}
