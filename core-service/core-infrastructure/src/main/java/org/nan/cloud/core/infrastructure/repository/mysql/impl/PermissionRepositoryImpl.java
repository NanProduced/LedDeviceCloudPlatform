package org.nan.cloud.core.infrastructure.repository.mysql.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.nan.cloud.core.domain.Permission;
import org.nan.cloud.core.infrastructure.repository.mysql.DO.PermissionDO;
import org.nan.cloud.core.infrastructure.repository.mysql.converter.CommonConverter;
import org.nan.cloud.core.infrastructure.repository.mysql.mapper.PermissionMapper;
import org.nan.cloud.core.infrastructure.repository.mysql.mapper.RoleMapper;
import org.nan.cloud.core.repository.PermissionRepository;
import org.springframework.stereotype.Repository;

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
                .eq(PermissionDO::getType, 1));
        return commonConverter.permissionDO2Permission(allPermissions);


    }

    @Override
    public List<Permission> getPermissionsByIds(List<Long> permissionIds) {
        final List<PermissionDO> permissionDOS = permissionMapper.selectList(new LambdaQueryWrapper<PermissionDO>()
                .eq(PermissionDO::getType, 1)
                .eq(PermissionDO::getPermissionId, permissionIds));
        return commonConverter.permissionDO2Permission(permissionDOS);
    }

    @Override
    public List<Permission> getPermissionsByRids(Long oid, List<Long> rids) {
        Set<Long> permissionIds = permissionMapper.getPermissionIdsByRids(oid, rids);
        return commonConverter.permissionDO2Permission(permissionMapper.selectByIds(permissionIds));
    }

    @Override
    public List<Long> getPermissionIdsByRid(Long rid) {
        return permissionMapper.getPermissionIdByRid(rid);
    }

    @Override
    public void insertRolePermissionRel(Long rid, Set<Long> permissionIds) {
        permissionMapper.insertRolePermissions(rid, permissionIds);
    }

    @Override
    public Set<Long> getPermissionIdsByRids(Long oid, List<Long> rids) {
        return permissionMapper.getPermissionIdsByRids(oid, rids);
    }

    @Override
    public Set<Long> getPermissionIdsByUid(Long uid) {
        return permissionMapper.getPermissionIdsByUid(uid);
    }
}
