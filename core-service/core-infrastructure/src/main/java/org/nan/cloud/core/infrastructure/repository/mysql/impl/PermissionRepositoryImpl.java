package org.nan.cloud.core.infrastructure.repository.mysql.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.nan.cloud.core.domain.Permission;
import org.nan.cloud.core.infrastructure.repository.mysql.DO.PermissionDO;
import org.nan.cloud.core.infrastructure.repository.mysql.converter.CommonConverter;
import org.nan.cloud.core.infrastructure.repository.mysql.mapper.PermissionMapper;
import org.nan.cloud.core.repository.PermissionRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Set;

@Repository
@RequiredArgsConstructor
public class PermissionRepositoryImpl implements PermissionRepository {

    private final PermissionMapper permissionMapper;

    private final CommonConverter commonConverter;


    @Override
    public List<Permission> getAllPermissions() {
        List<PermissionDO> allPermissions = permissionMapper.selectList(new LambdaQueryWrapper<PermissionDO>()
                .eq(PermissionDO::getPermissionType, 1));
        return commonConverter.permissionDO2Permission(allPermissions);


    }

    @Override
    public List<Permission> getPermissionsByIds(Collection<Long> permissionIds) {
        final List<PermissionDO> permissionDOS = permissionMapper.selectList(new LambdaQueryWrapper<PermissionDO>()
                .eq(PermissionDO::getPermissionType, 1)
                .in(PermissionDO::getPermissionId, permissionIds));
        return commonConverter.permissionDO2Permission(permissionDOS);
    }
}
