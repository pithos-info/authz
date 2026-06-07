package info.pithos.rbac.impl;

import info.pithos.data.relational.client.RelationalClient;
import info.pithos.rbac.AbstractRbacService;
import info.pithos.rbac.RbacService;
import info.pithos.rbac.model.Rbac;
import info.pithos.runtime.model.protocol.Context.RequestContext;
import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class RelationalRbacService extends AbstractRbacService implements RbacService {

    private final String changelogPath;

    public RelationalRbacService(RelationalClient relationalClient, String changelogPath) {
        super(relationalClient);
        this.changelogPath = changelogPath;
    }

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    @Override
    public CompletableFuture<Boolean> start(long timeout, TimeUnit unit) {
        return relationalClient.start(timeout, unit)
            .thenCompose(started -> relationalClient.transaction(conn -> {
                Database db = DatabaseFactory.getInstance()
                    .findCorrectDatabaseImplementation(new JdbcConnection(conn));
                new Liquibase(changelogPath, new ClassLoaderResourceAccessor(), db)
                    .update(new Contexts(), new LabelExpression());
            }))
            .thenApply(v -> true);
    }

    @Override
    public CompletableFuture<Boolean> shutdown(long timeout, TimeUnit unit) {
        return relationalClient.shutdown(timeout, unit);
    }

    // -------------------------------------------------------------------------
    // Group membership
    // -------------------------------------------------------------------------

    @Override
    public CompletableFuture<Void> addUserToGroup(RequestContext rc, String groupId, String userId) {
        return relationalClient.execute(dc(rc),
            "INSERT INTO \"groupMembers\" (\"groupId\", \"userId\") VALUES (?, ?) ON CONFLICT DO NOTHING",
            UUID.fromString(groupId), UUID.fromString(userId))
            .thenAccept(n -> {});
    }

    @Override
    public CompletableFuture<Void> removeUserFromGroup(RequestContext rc, String groupId, String userId) {
        return relationalClient.execute(dc(rc),
            "DELETE FROM \"groupMembers\" WHERE \"groupId\" = ? AND \"userId\" = ?",
            UUID.fromString(groupId), UUID.fromString(userId))
            .thenAccept(n -> {});
    }

    @Override
    public CompletableFuture<List<Rbac.User>> getGroupMembers(RequestContext rc, String groupId) {
        return relationalClient.query(dc(rc),
            """
            SELECT u.id, u."enterpriseId", u.email, u."externalId", u."idpProvider", u."displayName", u."lastLoginAt", u."utcCreatedAt", u.deleted
            FROM "user" u
            JOIN "groupMembers" gm ON gm."userId" = u.id
            WHERE gm."groupId" = ? AND u.deleted = false
            ORDER BY u.email
            """, UUID.fromString(groupId))
            .thenApply(rows -> rows.stream().map(RelationalUserService::toUser).toList());
    }

    @Override
    public CompletableFuture<List<Rbac.Group>> getUserGroups(RequestContext rc) {
        return relationalClient.query(dc(rc),
            """
            SELECT g.id, g."enterpriseId", g.name, g."utcCreatedAt", g.deleted
            FROM groups g
            JOIN "groupMembers" gm ON gm."groupId" = g.id
            WHERE gm."userId" = ? AND g."enterpriseId" = ? AND g.deleted = false
            ORDER BY g.name
            """, authUserId(rc), authEnterpriseId(rc))
            .thenApply(rows -> rows.stream().map(RelationalGroupService::toGroup).toList());
    }

    @Override
    public CompletableFuture<Boolean> isUserInGroup(RequestContext rc, String groupId) {
        return relationalClient.query(dc(rc),
            """
            SELECT EXISTS (
                SELECT 1 FROM "groupMembers" gm
                JOIN groups g ON g.id = gm."groupId"
                WHERE gm."userId" = ? AND gm."groupId" = ? AND g."enterpriseId" = ? AND g.deleted = false
            ) AS result
            """, authUserId(rc), UUID.fromString(groupId), authEnterpriseId(rc))
            .thenApply(rows -> rows.get(0).getBoolean("result"));
    }

    // -------------------------------------------------------------------------
    // Role assignment
    // -------------------------------------------------------------------------

    @Override
    public CompletableFuture<Void> assignRoleToUser(RequestContext rc, String userId, String roleId) {
        return relationalClient.execute(dc(rc),
            "INSERT INTO \"userRoles\" (\"userId\", \"roleId\", \"grantedBy\") VALUES (?, ?, ?) ON CONFLICT DO NOTHING",
            UUID.fromString(userId), UUID.fromString(roleId), authUserId(rc))
            .thenAccept(n -> {});
    }

    @Override
    public CompletableFuture<Void> revokeRoleFromUser(RequestContext rc, String userId, String roleId) {
        return relationalClient.execute(dc(rc),
            "DELETE FROM \"userRoles\" WHERE \"userId\" = ? AND \"roleId\" = ?",
            UUID.fromString(userId), UUID.fromString(roleId))
            .thenAccept(n -> {});
    }

    @Override
    public CompletableFuture<List<Rbac.Role>> getUserRoles(RequestContext rc) {
        UUID uid = authUserId(rc);
        return relationalClient.query(dc(rc),
            """
            SELECT DISTINCT r.id, r."enterpriseId", r.name, r."utcCreatedAt", r.deleted
            FROM roles r
            WHERE r.id IN (
                SELECT "roleId" FROM "userRoles" WHERE "userId" = ?
                UNION
                SELECT gr."roleId" FROM "groupRoles" gr
                JOIN "groupMembers" gm ON gm."groupId" = gr."groupId"
                WHERE gm."userId" = ?
            )
            AND r."enterpriseId" = ? AND r.deleted = false
            ORDER BY r.name
            """, uid, uid, authEnterpriseId(rc))
            .thenApply(rows -> rows.stream().map(RelationalRoleService::toRole).toList());
    }

    @Override
    public CompletableFuture<Void> assignRoleToGroup(RequestContext rc, String groupId, String roleId) {
        return relationalClient.execute(dc(rc),
            "INSERT INTO \"groupRoles\" (\"groupId\", \"roleId\") VALUES (?, ?) ON CONFLICT DO NOTHING",
            UUID.fromString(groupId), UUID.fromString(roleId))
            .thenAccept(n -> {});
    }

    @Override
    public CompletableFuture<Void> revokeRoleFromGroup(RequestContext rc, String groupId, String roleId) {
        return relationalClient.execute(dc(rc),
            "DELETE FROM \"groupRoles\" WHERE \"groupId\" = ? AND \"roleId\" = ?",
            UUID.fromString(groupId), UUID.fromString(roleId))
            .thenAccept(n -> {});
    }

    // -------------------------------------------------------------------------
    // Role permissions
    // -------------------------------------------------------------------------

    @Override
    public CompletableFuture<Void> addPermissionToRole(RequestContext rc, String roleId, String permission) {
        return relationalClient.execute(dc(rc),
            "INSERT INTO \"rolePermissions\" (\"roleId\", permission) VALUES (?, ?) ON CONFLICT DO NOTHING",
            UUID.fromString(roleId), permission)
            .thenAccept(n -> {});
    }

    @Override
    public CompletableFuture<Void> removePermissionFromRole(RequestContext rc, String roleId, String permission) {
        return relationalClient.execute(dc(rc),
            "DELETE FROM \"rolePermissions\" WHERE \"roleId\" = ? AND permission = ?",
            UUID.fromString(roleId), permission)
            .thenAccept(n -> {});
    }

    @Override
    public CompletableFuture<List<String>> getRolePermissions(RequestContext rc, String roleId) {
        return relationalClient.query(dc(rc),
            "SELECT permission FROM \"rolePermissions\" WHERE \"roleId\" = ? ORDER BY permission",
            UUID.fromString(roleId))
            .thenApply(rows -> rows.stream().map(r -> r.getString("permission")).toList());
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    @Override
    public CompletableFuture<Boolean> hasPermission(RequestContext rc, String permission) {
        UUID uid = authUserId(rc);
        return relationalClient.query(dc(rc),
            """
            SELECT (
                EXISTS (
                    SELECT 1 FROM "rolePermissions" rp
                    WHERE rp.permission = ?
                    AND rp."roleId" IN (
                        SELECT "roleId" FROM "userRoles" WHERE "userId" = ?
                        UNION
                        SELECT gr."roleId" FROM "groupRoles" gr
                        JOIN "groupMembers" gm ON gm."groupId" = gr."groupId"
                        WHERE gm."userId" = ?
                    )
                )
                AND EXISTS (SELECT 1 FROM "user" WHERE id = ? AND "enterpriseId" = ? AND deleted = false)
            ) AS result
            """, permission, uid, uid, uid, authEnterpriseId(rc))
            .thenApply(rows -> rows.get(0).getBoolean("result"));
    }

    @Override
    public CompletableFuture<Boolean> hasRole(RequestContext rc, String roleId) {
        UUID uid = authUserId(rc);
        UUID rid = UUID.fromString(roleId);
        return relationalClient.query(dc(rc),
            """
            SELECT (
                EXISTS (
                    SELECT 1 FROM "userRoles" WHERE "userId" = ? AND "roleId" = ?
                    UNION ALL
                    SELECT 1 FROM "groupRoles" gr
                    JOIN "groupMembers" gm ON gm."groupId" = gr."groupId"
                    WHERE gm."userId" = ? AND gr."roleId" = ?
                )
                AND EXISTS (SELECT 1 FROM "user" WHERE id = ? AND "enterpriseId" = ? AND deleted = false)
            ) AS result
            """, uid, rid, uid, rid, uid, authEnterpriseId(rc))
            .thenApply(rows -> rows.get(0).getBoolean("result"));
    }

    @Override
    public CompletableFuture<List<String>> getUserPermissions(RequestContext rc) {
        UUID uid = authUserId(rc);
        return relationalClient.query(dc(rc),
            """
            SELECT DISTINCT rp.permission
            FROM "rolePermissions" rp
            WHERE rp."roleId" IN (
                SELECT "roleId" FROM "userRoles" WHERE "userId" = ?
                UNION
                SELECT gr."roleId" FROM "groupRoles" gr
                JOIN "groupMembers" gm ON gm."groupId" = gr."groupId"
                WHERE gm."userId" = ?
            )
            AND EXISTS (SELECT 1 FROM "user" WHERE id = ? AND "enterpriseId" = ? AND deleted = false)
            ORDER BY rp.permission
            """, uid, uid, uid, authEnterpriseId(rc))
            .thenApply(rows -> rows.stream().map(r -> r.getString("permission")).toList());
    }
}
