package org.nan.cloud.core.repository;

import org.nan.cloud.core.domain.Permission;

import java.util.List;
import java.util.Set;

public interface PermissionRepository {

    List<Permission> getPermissionsByIds(List<Long> permissionIds);

    List<Permission> getPermissionsByRoles(List<Long> rids);

    void insertRolePermissionRel(Long rid, Set<Long> permissionIds);

    Set<Long> getPermissionIdsByRoles(Long oid, List<Long> rids);

    Set<Long> getPermissionIdsByUid(Long uid);
}
