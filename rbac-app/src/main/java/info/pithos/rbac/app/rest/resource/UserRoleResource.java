package info.pithos.rbac.app.rest.resource;

import com.google.inject.Inject;
import info.pithos.rbac.app.handler.UserRoleHandlers;
import info.pithos.rbac.service.GetByIdRequest;
import info.pithos.rbac.service.GrantUserRoleRequest;
import info.pithos.rbac.service.RevokeUserRoleRequest;
import info.pithos.service.container.core.BaseServiceHandler;
import io.vertx.ext.web.Router;

public final class UserRoleResource {

    private final UserRoleHandlers.Grant      grant;
    private final UserRoleHandlers.Revoke     revoke;
    private final UserRoleHandlers.ListByUser listByUser;

    @Inject
    public UserRoleResource(
            UserRoleHandlers.Grant      grant,
            UserRoleHandlers.Revoke     revoke,
            UserRoleHandlers.ListByUser listByUser) {
        this.grant      = grant;
        this.revoke     = revoke;
        this.listByUser = listByUser;
    }

    public void mount(Router r) {
        r.post("/users/:userId/roles").handler(ctx -> {
            GrantUserRoleRequest req = BaseServiceHandler.parseBody(ctx, GrantUserRoleRequest.newBuilder());
            if (req == null) return;
            BaseServiceHandler.route(ctx, 201, grant,
                req.toBuilder().setUserId(ctx.pathParam("userId")).build());
        });

        r.delete("/users/:userId/roles/:roleId").handler(ctx ->
            BaseServiceHandler.routeNoContent(ctx, revoke,
                RevokeUserRoleRequest.newBuilder()
                    .setUserId(ctx.pathParam("userId"))
                    .setRoleId(ctx.pathParam("roleId"))
                    .build()));

        r.get("/users/:userId/roles").handler(ctx ->
            BaseServiceHandler.route(ctx, 200, listByUser,
                GetByIdRequest.newBuilder().setId(ctx.pathParam("userId")).build()));
    }
}
