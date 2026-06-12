package info.pithos.rbac.impl;

import info.pithos.data.relational.Row;
import info.pithos.rbac.GroupMemberService;
import info.pithos.data.relational.client.RelationalClient;
import info.pithos.data.relational.client.ProtoBufAssociationService;
import info.pithos.rbac.model.Rbac;
import info.pithos.runtime.model.protocol.Context.RequestContext;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class RelationalGroupMemberService extends ProtoBufAssociationService<Rbac.GroupMember>
        implements GroupMemberService {

    public RelationalGroupMemberService(RelationalClient relationalClient) {
        super(relationalClient, "groupMember", Rbac.GroupMember.getDefaultInstance(), "groupId", "userId");
    }

    @Override
    protected Rbac.GroupMember mapRow(Row row) {
        return Rbac.GroupMember.newBuilder()
            .setEnterpriseId(row.getStr("enterpriseId"))
            .setGroupId(row.getStr("groupId"))
            .setUserId(row.getStr("userId"))
            .setUtcCreatedAt(row.getEpochMillis("utcCreatedAt"))
            .build();
    }

    @Override
    public CompletableFuture<Rbac.GroupMember> add(RequestContext rc, String groupId, String userId) {
        Rbac.GroupMember member = Rbac.GroupMember.newBuilder()
            .setEnterpriseId(authEnterpriseId(rc))
            .setGroupId(groupId)
            .setUserId(userId)
            .build();
        return insert(rc, member);
    }

    @Override
    public CompletableFuture<Void> remove(RequestContext rc, String groupId, String userId) {
        Rbac.GroupMember key = Rbac.GroupMember.newBuilder()
            .setGroupId(groupId).setUserId(userId).build();
        return deleteByKey(rc, key);
    }

    @Override
    public CompletableFuture<Optional<Rbac.GroupMember>> get(RequestContext rc, String groupId, String userId) {
        Rbac.GroupMember key = Rbac.GroupMember.newBuilder()
            .setGroupId(groupId).setUserId(userId).build();
        return getByKey(rc, key);
    }

    @Override
    public CompletableFuture<Boolean> isUserInGroup(RequestContext rc, String groupId) {
        return relationalClient.query(dc(rc),
            """
            SELECT EXISTS (
                SELECT 1 FROM "groupMember" gm
                JOIN "group" g ON g.id = gm."groupId"
                WHERE gm."userId" = ? AND gm."groupId" = ? AND g."enterpriseId" = ? AND g.deleted = false
            ) AS result
            """, authUserId(rc), groupId, authEnterpriseId(rc))
            .thenApply(rows -> rows.get(0).getBoolean("result"));
    }
}
