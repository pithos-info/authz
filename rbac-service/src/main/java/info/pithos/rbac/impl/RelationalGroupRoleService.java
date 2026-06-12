package info.pithos.rbac.impl;

import info.pithos.data.relational.Row;
import info.pithos.rbac.GroupRoleService;
import info.pithos.data.relational.client.RelationalClient;
import info.pithos.data.relational.client.ProtoBufAssociationService;
import info.pithos.rbac.model.Rbac;
import info.pithos.runtime.model.protocol.Context.RequestContext;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class RelationalGroupRoleService extends ProtoBufAssociationService<Rbac.GroupRole>
        implements GroupRoleService {

    public RelationalGroupRoleService(RelationalClient relationalClient) {
        super(relationalClient, "groupRole", Rbac.GroupRole.getDefaultInstance(), "groupId", "roleId");
    }

    @Override
    protected Rbac.GroupRole mapRow(Row row) {
        return Rbac.GroupRole.newBuilder()
            .setEnterpriseId(row.getStr("enterpriseId"))
            .setGroupId(row.getStr("groupId"))
            .setRoleId(row.getStr("roleId"))
            .setUtcCreatedAt(row.getEpochMillis("utcCreatedAt"))
            .build();
    }

    @Override
    public CompletableFuture<Rbac.GroupRole> assign(RequestContext rc, String groupId, String roleId) {
        Rbac.GroupRole groupRole = Rbac.GroupRole.newBuilder()
            .setEnterpriseId(authEnterpriseId(rc))
            .setGroupId(groupId)
            .setRoleId(roleId)
            .build();
        return insert(rc, groupRole);
    }

    @Override
    public CompletableFuture<Void> unassign(RequestContext rc, String groupId, String roleId) {
        Rbac.GroupRole key = Rbac.GroupRole.newBuilder()
            .setGroupId(groupId).setRoleId(roleId).build();
        return deleteByKey(rc, key);
    }

    @Override
    public CompletableFuture<Optional<Rbac.GroupRole>> get(RequestContext rc, String groupId, String roleId) {
        Rbac.GroupRole key = Rbac.GroupRole.newBuilder()
            .setGroupId(groupId).setRoleId(roleId).build();
        return getByKey(rc, key);
    }
}
