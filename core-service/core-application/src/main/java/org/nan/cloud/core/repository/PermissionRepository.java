package org.nan.cloud.core.repository;

import org.nan.cloud.core.domain.Permission;

import java.util.List;
import java.util.Set;

public interface PermissionRepository {

    List<Permission> getAllPermissions();

    List<Permission> getPermissionsByIds(List<Long> permissionIds);

    List<Permission> getPermissionsByRids(Long oid, List<Long> rids);

    List<Long> getPermissionIdsByRid(Long rid);

    void insertRolePermissionRel(Long rid, Set<Long> permissionIds);

    Set<Long> getPermissionIdsByRids(Long oid, List<Long> rids);

    Set<Long> getPermissionIdsByUid(Long uid);

}
