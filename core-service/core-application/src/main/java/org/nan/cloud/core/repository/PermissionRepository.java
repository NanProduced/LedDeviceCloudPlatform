package org.nan.cloud.core.repository;

import org.nan.cloud.core.domain.Permission;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface PermissionRepository {

    List<Permission> getAllPermissions();

    List<Permission> getPermissionsByIds(Collection<Long> permissionIds);

}
