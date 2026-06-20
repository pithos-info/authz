/*
 * Copyright 2026 Pithos
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package info.pithos.rbac.app.handler;

import info.pithos.data.relational.FilterCriteria;
import info.pithos.rbac.GroupRoleService;
import info.pithos.rbac.GroupService;
import info.pithos.rbac.RolePermissionService;
import info.pithos.rbac.RoleService;
import info.pithos.rbac.model.Rbac;
import info.pithos.rbac.service.Enterprise;
import info.pithos.rbac.service.Group;
import info.pithos.rbac.service.Role;
import info.pithos.runtime.model.protocol.Context.RequestContext;
import info.pithos.serde.ProtoBufMapper;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Async helpers that build enriched service-API types from data-layer objects.
 * All methods return {@link CompletableFuture} so callers can compose without
 * blocking the Vert.x event loop.
 */
final class RbacEnricher {

    /** Role with its permissions populated. */
    static CompletableFuture<Role> apiRole(
            RequestContext rc, Rbac.Role data, RolePermissionService rpService) {
        return rpService.select(rc, FilterCriteria.eq("roleId", data.getId()))
            .thenApply(perms -> {
                List<String> ps = perms.stream()
                    .map(Rbac.RolePermission::getPermission)
                    .toList();
                return ProtoBufMapper.<Role>map(data, Role.newBuilder())
                    .toBuilder().addAllPermissions(ps).build();
            });
    }

    /** List of roles, each with permissions. Requests run in parallel. */
    @SuppressWarnings("unchecked")
    static CompletableFuture<List<Role>> apiRoles(
            RequestContext rc, List<Rbac.Role> dataRoles, RolePermissionService rpService) {
        if (dataRoles.isEmpty()) return CompletableFuture.completedFuture(List.of());
        CompletableFuture<Role>[] futures = dataRoles.stream()
            .map(r -> apiRole(rc, r, rpService))
            .toArray(CompletableFuture[]::new);
        return CompletableFuture.allOf(futures)
            .thenApply(v -> Arrays.stream(futures).map(CompletableFuture::join).toList());
    }

    /** Group with its assigned roles, each role carrying permissions. */
    static CompletableFuture<Group> apiGroup(
            RequestContext rc, Rbac.Group data,
            GroupRoleService grService, RoleService roleService, RolePermissionService rpService) {
        return grService.select(rc, FilterCriteria.eq("groupId", data.getId()))
            .thenCompose(groupRoles -> {
                @SuppressWarnings("unchecked")
                CompletableFuture<Rbac.Role>[] roleFetches = groupRoles.stream()
                    .map(gr -> roleService.get(rc, gr.getRoleId())
                        .thenApply(opt -> opt.orElse(null)))
                    .toArray(CompletableFuture[]::new);
                return CompletableFuture.allOf(roleFetches)
                    .thenCompose(v -> {
                        List<Rbac.Role> dataRoles = Arrays.stream(roleFetches)
                            .map(CompletableFuture::join)
                            .filter(r -> r != null)
                            .toList();
                        return apiRoles(rc, dataRoles, rpService);
                    });
            })
            .thenApply(roles ->
                ProtoBufMapper.<Group>map(data, Group.newBuilder())
                    .toBuilder().addAllRoles(roles).build()
            );
    }

    /** List of groups, each enriched with roles. Requests run in parallel. */
    @SuppressWarnings("unchecked")
    static CompletableFuture<List<Group>> apiGroups(
            RequestContext rc, List<Rbac.Group> dataGroups,
            GroupRoleService grService, RoleService roleService, RolePermissionService rpService) {
        if (dataGroups.isEmpty()) return CompletableFuture.completedFuture(List.of());
        CompletableFuture<Group>[] futures = dataGroups.stream()
            .map(g -> apiGroup(rc, g, grService, roleService, rpService))
            .toArray(CompletableFuture[]::new);
        return CompletableFuture.allOf(futures)
            .thenApply(v -> Arrays.stream(futures).map(CompletableFuture::join).toList());
    }

    /** Enterprise hydrated with its groups (each with roles+permissions) and roles (with permissions). */
    static CompletableFuture<Enterprise> apiEnterprise(
            RequestContext rc, Rbac.Enterprise data,
            GroupService groupService, GroupRoleService groupRoleService,
            RoleService roleService, RolePermissionService rpService) {
        CompletableFuture<List<Group>> groupsFuture = groupService.list(rc)
            .thenCompose(gs -> apiGroups(rc, gs, groupRoleService, roleService, rpService));
        CompletableFuture<List<Role>> rolesFuture = roleService.list(rc)
            .thenCompose(rs -> apiRoles(rc, rs, rpService));
        return groupsFuture.thenCombine(rolesFuture, (groups, roles) ->
            ProtoBufMapper.<Enterprise>map(data, Enterprise.newBuilder())
                .toBuilder()
                .addAllGroups(groups)
                .addAllRoles(roles)
                .build());
    }

    private RbacEnricher() {}
}
