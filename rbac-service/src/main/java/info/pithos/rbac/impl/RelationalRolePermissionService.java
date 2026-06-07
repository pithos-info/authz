package info.pithos.rbac.impl;

import info.pithos.data.relational.FilterCriteria;
import info.pithos.data.relational.Row;
import info.pithos.data.relational.client.ProtoBufRelationalClient;
import info.pithos.data.relational.client.ProtoBufStatement;
import info.pithos.data.relational.client.RelationalClient;
import info.pithos.rbac.AbstractRbacService;
import info.pithos.rbac.RolePermissionService;
import info.pithos.rbac.model.Rbac;
import info.pithos.runtime.model.protocol.Context.RequestContext;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class RelationalRolePermissionService extends AbstractRbacService implements RolePermissionService {

    private static final ProtoBufStatement<Rbac.RolePermission> STMT =
        ProtoBufStatement.of("rolePermission", Rbac.RolePermission.getDefaultInstance(),
                             new String[]{"roleId", "permission"});

    private final ProtoBufRelationalClient<Rbac.RolePermission> store;

    public RelationalRolePermissionService(RelationalClient relationalClient) {
        super(relationalClient);
        this.store = ProtoBufRelationalClient.forFilter(relationalClient, "rolePermission",
                                                       Rbac.RolePermission.getDefaultInstance(), "roleId");
    }

    @Override
    public CompletableFuture<Rbac.RolePermission> add(RequestContext rc, String roleId, String permission) {
        Rbac.RolePermission rolePermission = Rbac.RolePermission.newBuilder()
            .setRoleId(roleId)
            .setPermission(permission)
            .setUtcCreatedAt(System.currentTimeMillis())
            .build();
        return relationalClient.query(dc(rc), STMT.insert(rolePermission))
            .thenApply(rows -> toRolePermission(rows.get(0)));
    }

    @Override
    public CompletableFuture<Void> remove(RequestContext rc, String roleId, String permission) {
        Rbac.RolePermission key = Rbac.RolePermission.newBuilder()
            .setRoleId(roleId)
            .setPermission(permission)
            .build();
        return relationalClient.execute(dc(rc), STMT.deleteByCompositeId(key)).thenAccept(n -> {});
    }

    @Override
    public CompletableFuture<Optional<Rbac.RolePermission>> get(RequestContext rc, String roleId, String permission) {
        Rbac.RolePermission key = Rbac.RolePermission.newBuilder()
            .setRoleId(roleId)
            .setPermission(permission)
            .build();
        return relationalClient.query(dc(rc), STMT.selectByCompositeId(key))
            .thenApply(rows -> rows.isEmpty() ? Optional.empty() : Optional.of(toRolePermission(rows.get(0))));
    }

    @Override
    public CompletableFuture<List<Rbac.RolePermission>> select(RequestContext rc, FilterCriteria filter) {
        return store.findAll(dc(rc), filter);
    }

    @Override
    public CompletableFuture<Boolean> hasPermission(RequestContext rc, String permission) {
        UUID uid = authUserId(rc);
        return relationalClient.query(dc(rc),
            """
            SELECT (
                EXISTS (
                    SELECT 1 FROM "rolePermission" rp
                    WHERE rp.permission = ?
                    AND rp."roleId" IN (
                        SELECT "roleId" FROM "userRole" WHERE "userId" = ?
                        UNION
                        SELECT gr."roleId" FROM "groupRole" gr
                        JOIN "groupMember" gm ON gm."groupId" = gr."groupId"
                        WHERE gm."userId" = ?
                    )
                )
                AND EXISTS (SELECT 1 FROM "user" WHERE id = ? AND "enterpriseId" = ? AND deleted = false)
            ) AS result
            """, permission, uid, uid, uid, authEnterpriseId(rc))
            .thenApply(rows -> rows.get(0).getBoolean("result"));
    }

    @Override
    public CompletableFuture<List<String>> getUserPermissions(RequestContext rc) {
        UUID uid = authUserId(rc);
        return relationalClient.query(dc(rc),
            """
            SELECT DISTINCT rp.permission
            FROM "rolePermission" rp
            WHERE rp."roleId" IN (
                SELECT "roleId" FROM "userRole" WHERE "userId" = ?
                UNION
                SELECT gr."roleId" FROM "groupRole" gr
                JOIN "groupMember" gm ON gm."groupId" = gr."groupId"
                WHERE gm."userId" = ?
            )
            AND EXISTS (SELECT 1 FROM "user" WHERE id = ? AND "enterpriseId" = ? AND deleted = false)
            ORDER BY rp.permission
            """, uid, uid, uid, authEnterpriseId(rc))
            .thenApply(rows -> rows.stream().map(r -> r.getString("permission")).toList());
    }

    private static Rbac.RolePermission toRolePermission(Row row) {
        return Rbac.RolePermission.newBuilder()
            .setRoleId(row.getStr("roleId"))
            .setPermission(row.getString("permission"))
            .setUtcCreatedAt(row.getEpochMillis("utcCreatedAt"))
            .build();
    }
}
