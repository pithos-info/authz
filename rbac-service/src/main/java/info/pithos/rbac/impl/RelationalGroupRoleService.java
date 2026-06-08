package info.pithos.rbac.impl;

import info.pithos.data.relational.FilterCriteria;
import info.pithos.data.relational.Row;
import info.pithos.data.relational.client.ProtoBufRelationalClient;
import info.pithos.data.relational.client.ProtoBufStatement;
import info.pithos.data.relational.client.RelationalClient;
import info.pithos.rbac.AbstractRbacService;
import info.pithos.rbac.GroupRoleService;
import info.pithos.rbac.model.Rbac;
import info.pithos.runtime.model.protocol.Context.RequestContext;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class RelationalGroupRoleService extends AbstractRbacService implements GroupRoleService {

    private static final ProtoBufStatement<Rbac.GroupRole> STMT =
        ProtoBufStatement.of("groupRole", Rbac.GroupRole.getDefaultInstance(),
                             new String[]{"groupId", "roleId"});

    private final ProtoBufRelationalClient<Rbac.GroupRole> store;

    public RelationalGroupRoleService(RelationalClient relationalClient) {
        super(relationalClient);
        this.store = ProtoBufRelationalClient.forFilter(relationalClient, "groupRole",
                                                       Rbac.GroupRole.getDefaultInstance(), "groupId");
    }

    @Override
    public CompletableFuture<Rbac.GroupRole> assign(RequestContext rc, String groupId, String roleId) {
        Rbac.GroupRole groupRole = Rbac.GroupRole.newBuilder()
            .setGroupId(groupId)
            .setRoleId(roleId)
            .build();
        return relationalClient.query(dc(rc), STMT.insert(groupRole))
            .thenApply(rows -> toGroupRole(rows.get(0)));
    }

    @Override
    public CompletableFuture<Void> unassign(RequestContext rc, String groupId, String roleId) {
        Rbac.GroupRole key = Rbac.GroupRole.newBuilder()
            .setGroupId(groupId)
            .setRoleId(roleId)
            .build();
        return relationalClient.execute(dc(rc), STMT.deleteByCompositeId(key)).thenAccept(n -> {});
    }

    @Override
    public CompletableFuture<Optional<Rbac.GroupRole>> get(RequestContext rc, String groupId, String roleId) {
        Rbac.GroupRole key = Rbac.GroupRole.newBuilder()
            .setGroupId(groupId)
            .setRoleId(roleId)
            .build();
        return relationalClient.query(dc(rc), STMT.selectByCompositeId(key))
            .thenApply(rows -> rows.isEmpty() ? Optional.empty() : Optional.of(toGroupRole(rows.get(0))));
    }

    @Override
    public CompletableFuture<List<Rbac.GroupRole>> select(RequestContext rc, FilterCriteria filter) {
        return store.findAll(dc(rc), filter);
    }

    private static Rbac.GroupRole toGroupRole(Row row) {
        return Rbac.GroupRole.newBuilder()
            .setGroupId(row.getStr("groupId"))
            .setRoleId(row.getStr("roleId"))
            .setUtcCreatedAt(row.getEpochMillis("utcCreatedAt"))
            .build();
    }
}
