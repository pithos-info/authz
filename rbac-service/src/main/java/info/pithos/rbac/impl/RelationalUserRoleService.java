package info.pithos.rbac.impl;

import info.pithos.data.relational.FilterCriteria;
import info.pithos.data.relational.Row;
import info.pithos.data.relational.client.ProtoBufRelationalClient;
import info.pithos.data.relational.client.ProtoBufStatement;
import info.pithos.data.relational.client.RelationalClient;
import info.pithos.rbac.AbstractRbacService;
import info.pithos.rbac.UserRoleService;
import info.pithos.rbac.model.Rbac;
import info.pithos.runtime.model.protocol.Context.RequestContext;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class RelationalUserRoleService extends AbstractRbacService implements UserRoleService {

    private static final ProtoBufStatement<Rbac.UserRole> STMT =
        ProtoBufStatement.of("userRole", Rbac.UserRole.getDefaultInstance(),
                             new String[]{"userId", "roleId"});

    private final ProtoBufRelationalClient<Rbac.UserRole> store;

    public RelationalUserRoleService(RelationalClient relationalClient) {
        super(relationalClient);
        this.store = ProtoBufRelationalClient.forFilter(relationalClient, "userRole",
                                                       Rbac.UserRole.getDefaultInstance(), "userId");
    }

    @Override
    public CompletableFuture<Rbac.UserRole> grant(RequestContext rc, String userId, String roleId) {
        Rbac.UserRole userRole = Rbac.UserRole.newBuilder()
            .setEnterpriseId(rc.getAuthContext().getEnterpriseId())
            .setUserId(userId)
            .setRoleId(roleId)
            .setGrantedById(authUserId(rc).toString())
            .build();
        return relationalClient.query(dc(rc), STMT.insert(userRole))
            .thenApply(rows -> toUserRole(rows.get(0)));
    }

    @Override
    public CompletableFuture<Void> revoke(RequestContext rc, String userId, String roleId) {
        Rbac.UserRole key = Rbac.UserRole.newBuilder()
            .setUserId(userId)
            .setRoleId(roleId)
            .build();
        return relationalClient.execute(dc(rc), STMT.deleteByCompositeId(key)).thenAccept(n -> {});
    }

    @Override
    public CompletableFuture<Optional<Rbac.UserRole>> get(RequestContext rc, String userId, String roleId) {
        Rbac.UserRole key = Rbac.UserRole.newBuilder()
            .setUserId(userId)
            .setRoleId(roleId)
            .build();
        return relationalClient.query(dc(rc), STMT.selectByCompositeId(key))
            .thenApply(rows -> rows.isEmpty() ? Optional.empty() : Optional.of(toUserRole(rows.get(0))));
    }

    @Override
    public CompletableFuture<List<Rbac.UserRole>> select(RequestContext rc, FilterCriteria filter) {
        return store.findAll(dc(rc), filter);
    }

    @Override
    public CompletableFuture<Boolean> hasRole(RequestContext rc, String roleId) {
        UUID uid = authUserId(rc);
        UUID rid = UUID.fromString(roleId);
        return relationalClient.query(dc(rc),
            """
            SELECT (
                EXISTS (
                    SELECT 1 FROM "userRole" WHERE "userId" = ? AND "roleId" = ?
                    UNION ALL
                    SELECT 1 FROM "groupRole" gr
                    JOIN "groupMember" gm ON gm."groupId" = gr."groupId"
                    WHERE gm."userId" = ? AND gr."roleId" = ?
                )
                AND EXISTS (SELECT 1 FROM "user" WHERE id = ? AND "enterpriseId" = ? AND deleted = false)
            ) AS result
            """, uid, rid, uid, rid, uid, authEnterpriseId(rc))
            .thenApply(rows -> rows.get(0).getBoolean("result"));
    }

    private static Rbac.UserRole toUserRole(Row row) {
        Rbac.UserRole.Builder b = Rbac.UserRole.newBuilder()
            .setEnterpriseId(row.getStr("enterpriseId"))
            .setUserId(row.getStr("userId"))
            .setRoleId(row.getStr("roleId"))
            .setUtcCreatedAt(row.getEpochMillis("utcCreatedAt"));
        String grantedById = row.getStr("grantedById");
        if (grantedById != null) b.setGrantedById(grantedById);
        return b.build();
    }
}
