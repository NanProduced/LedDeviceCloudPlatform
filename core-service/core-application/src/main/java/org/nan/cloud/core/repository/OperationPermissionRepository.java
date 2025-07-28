package org.nan.cloud.core.repository;

import org.nan.cloud.core.domain.OperationPermission;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface OperationPermissionRepository {

    void insertRoleOperationRel(Long rid, Set<Long> operationPermissionIds);

    void updateRoleOperationRel(Long rid, Set<Long> operationPermissionIds);

    List<OperationPermission> getOperationPermissionByRid(Long rid);

    List<OperationPermission> getOperationPermissionByRids(List<Long> rids);

    List<Long> getOperationPermissionIdByRids(List<Long> rids);

    Map<Long, String> getPermissionsByOperationPermissionId(Long rid);

    Set<Long> getPermissionIdsByOperationPermissionIds(List<Long> operationPermissionIds);

    List<OperationPermission> getAllOperationPermissions();

}
