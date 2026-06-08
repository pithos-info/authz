package info.pithos.rbac.impl;

import info.pithos.data.relational.FilterCriteria;
import info.pithos.data.relational.Row;
import info.pithos.data.relational.client.ProtoBufRelationalClient;
import info.pithos.data.relational.client.ProtoBufStatement;
import info.pithos.data.relational.client.RelationalClient;
import info.pithos.rbac.AbstractRbacService;
import info.pithos.rbac.GroupMemberService;
import info.pithos.rbac.model.Rbac;
import info.pithos.runtime.model.protocol.Context.RequestContext;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class RelationalGroupMemberService extends AbstractRbacService implements GroupMemberService {

    private static final ProtoBufStatement<Rbac.GroupMember> STMT =
        ProtoBufStatement.of("groupMember", Rbac.GroupMember.getDefaultInstance(),
                             new String[]{"groupId", "userId"});

    private final ProtoBufRelationalClient<Rbac.GroupMember> store;

    public RelationalGroupMemberService(RelationalClient relationalClient) {
        super(relationalClient);
        this.store = ProtoBufRelationalClient.forFilter(relationalClient, "groupMember",
                                                       Rbac.GroupMember.getDefaultInstance(), "groupId");
    }

    @Override
    public CompletableFuture<Rbac.GroupMember> add(RequestContext rc, String groupId, String userId) {
        Rbac.GroupMember member = Rbac.GroupMember.newBuilder()
            .setGroupId(groupId)
            .setUserId(userId)
            .build();
        return relationalClient.query(dc(rc), STMT.insert(member))
            .thenApply(rows -> toGroupMember(rows.get(0)));
    }

    @Override
    public CompletableFuture<Void> remove(RequestContext rc, String groupId, String userId) {
        Rbac.GroupMember key = Rbac.GroupMember.newBuilder()
            .setGroupId(groupId)
            .setUserId(userId)
            .build();
        return relationalClient.execute(dc(rc), STMT.deleteByCompositeId(key)).thenAccept(n -> {});
    }

    @Override
    public CompletableFuture<Optional<Rbac.GroupMember>> get(RequestContext rc, String groupId, String userId) {
        Rbac.GroupMember key = Rbac.GroupMember.newBuilder()
            .setGroupId(groupId)
            .setUserId(userId)
            .build();
        return relationalClient.query(dc(rc), STMT.selectByCompositeId(key))
            .thenApply(rows -> rows.isEmpty() ? Optional.empty() : Optional.of(toGroupMember(rows.get(0))));
    }

    @Override
    public CompletableFuture<List<Rbac.GroupMember>> select(RequestContext rc, FilterCriteria filter) {
        return store.findAll(dc(rc), filter);
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
            """, authUserId(rc), UUID.fromString(groupId), authEnterpriseId(rc))
            .thenApply(rows -> rows.get(0).getBoolean("result"));
    }

    private static Rbac.GroupMember toGroupMember(Row row) {
        return Rbac.GroupMember.newBuilder()
            .setGroupId(row.getStr("groupId"))
            .setUserId(row.getStr("userId"))
            .setUtcCreatedAt(row.getEpochMillis("utcCreatedAt"))
            .build();
    }
}
