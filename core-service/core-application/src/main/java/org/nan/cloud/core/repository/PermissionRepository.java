package org.nan.cloud.core.repository;

import org.nan.cloud.core.domain.Permission;

import java.util.List;

public interface PermissionRepository {

    List<Permission> getPermissionsByIds(List<Long> permissionIds);
}
