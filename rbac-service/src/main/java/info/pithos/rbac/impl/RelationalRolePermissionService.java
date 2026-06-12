package info.pithos.rbac.impl;

import info.pithos.data.relational.Row;
import info.pithos.data.relational.client.RelationalClient;
import info.pithos.data.relational.client.ProtoBufAssociationService;
import info.pithos.rbac.RolePermissionService;
import info.pithos.rbac.model.Rbac;
import info.pithos.runtime.model.protocol.Context.RequestContext;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class RelationalRolePermissionService extends ProtoBufAssociationService<Rbac.RolePermission>
        implements RolePermissionService {

    public RelationalRolePermissionService(RelationalClient relationalClient) {
        super(relationalClient, "rolePermission", Rbac.RolePermission.getDefaultInstance(), "roleId", "permission");
    }

    @Override
    protected Rbac.RolePermission mapRow(Row row) {
        return Rbac.RolePermission.newBuilder()
            .setEnterpriseId(row.getStr("enterpriseId"))
            .setRoleId(row.getStr("roleId"))
            .setPermission(row.getString("permission"))
            .setUtcCreatedAt(row.getEpochMillis("utcCreatedAt"))
            .build();
    }

    @Override
    public CompletableFuture<Rbac.RolePermission> add(RequestContext rc, String roleId, String permission) {
        Rbac.RolePermission rp = Rbac.RolePermission.newBuilder()
            .setEnterpriseId(authEnterpriseId(rc))
            .setRoleId(roleId)
            .setPermission(permission)
            .build();
        return insert(rc, rp);
    }

    @Override
    public CompletableFuture<Void> remove(RequestContext rc, String roleId, String permission) {
        Rbac.RolePermission key = Rbac.RolePermission.newBuilder()
            .setRoleId(roleId).setPermission(permission).build();
        return deleteByKey(rc, key);
    }

    @Override
    public CompletableFuture<Optional<Rbac.RolePermission>> get(RequestContext rc, String roleId, String permission) {
        Rbac.RolePermission key = Rbac.RolePermission.newBuilder()
            .setRoleId(roleId).setPermission(permission).build();
        return getByKey(rc, key);
    }

    @Override
    public CompletableFuture<Boolean> hasPermission(RequestContext rc, String permission) {
        String uid = authUserId(rc);
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
        String uid = authUserId(rc);
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
}
