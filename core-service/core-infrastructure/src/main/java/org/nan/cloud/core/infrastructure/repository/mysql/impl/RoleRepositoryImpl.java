package org.nan.cloud.core.infrastructure.repository.mysql.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import org.nan.cloud.core.domain.Role;
import org.nan.cloud.core.infrastructure.repository.mysql.DO.RoleDO;
import org.nan.cloud.core.infrastructure.repository.mysql.converter.CommonConverter;
import org.nan.cloud.core.infrastructure.repository.mysql.mapper.RoleMapper;
import org.nan.cloud.core.repository.RoleRepository;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class RoleRepositoryImpl implements RoleRepository {

    private final RoleMapper roleMapper;

    private final CommonConverter commonConverter;

    @Override
    public Role createRole(Role role) {
        RoleDO roleDO = commonConverter.role2RoleDO(role);
        roleDO.setCreateTime(LocalDateTime.now());
        roleMapper.insert(roleDO);
        return commonConverter.roleDO2Role(roleDO);
    }

    @Override
    public Role getRoleByRid(Long rid) {
        return commonConverter.roleDO2Role(roleMapper.selectById(rid));
    }

    @Override
    public List<Long> getRidsByUid(Long uid) {
        return roleMapper.getRidsByUid(uid);
    }

    @Override
    public List<Role> getRolesByUid(Long uid) {
        List<Long> rids = roleMapper.getRidsByUid(uid);
        List<RoleDO> roleDOS = roleMapper.selectByIds(rids);
        return commonConverter.roleDO2Role(roleDOS);
    }

    @Override
    public void updateRole(Role role) throws DuplicateKeyException {
        RoleDO roleDO = commonConverter.role2RoleDO(role);
        roleDO.setUpdateTime(LocalDateTime.now());
        roleMapper.updateById(roleDO);
    }


    @Override
    public boolean allRolesExist(List<Long> roles) {
        return roleMapper.selectCount(new LambdaQueryWrapper<RoleDO>()
                .eq(RoleDO::getRid, roles)) == roles.size();
    }

    @Override
    public List<Role> getRolesByRids(Collection<Long> rids) {
        return commonConverter.roleDO2Role(roleMapper.selectByIds(rids));
    }

    @Override
    public Map<Long, List<Long>> getRoleIdsByUserIds(List<Long> userIds) {
        List<Map<String, Object>> rows = roleMapper.getRolesByUserIds(userIds);
        return rows.stream().collect(Collectors.groupingBy(
                row -> Long.parseLong(row.get("uid").toString()),
                Collectors.mapping(
                        row -> Long.parseLong(row.get("rid").toString()),
                        Collectors.toList()
                )
        ));
    }

    @Override
    public List<Role> getCoveredRolesByRids(Collection<Long> rids, Long oid) {
        return commonConverter.roleDO2Role(roleMapper.selectCoveredRoles(rids, oid));
    }

    @Override
    public boolean ifTheSameOrg(Long oid, Long rid) {
        return roleMapper.exists(new LambdaQueryWrapper<RoleDO>()
                .eq(RoleDO::getOid, oid)
                .eq(RoleDO::getRid, rid));
    }

    @Override
    public List<Long> getUserWithOnlyRole(Long oid, Long rid) {
        return roleMapper.getUserWithOnlyRole(oid, rid);
    }
}
