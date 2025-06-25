package org.nan.cloud.core.infrastructure.repository.mysql.impl;

import lombok.RequiredArgsConstructor;
import org.nan.cloud.core.domain.Permission;
import org.nan.cloud.core.infrastructure.repository.mysql.DO.PermissionDO;
import org.nan.cloud.core.infrastructure.repository.mysql.converter.CommonConverter;
import org.nan.cloud.core.infrastructure.repository.mysql.mapper.PermissionMapper;
import org.nan.cloud.core.repository.PermissionRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class PermissionRepositoryImpl implements PermissionRepository {

    private final PermissionMapper permissionMapper;

    private final CommonConverter commonConverter;

    @Override
    public List<Permission> getPermissionsByIds(List<Long> permissionIds) {
        final List<PermissionDO> permissionDOS = permissionMapper.selectByIds(permissionIds);
        return commonConverter.permissionDO2Permission(permissionDOS);
    }
}
