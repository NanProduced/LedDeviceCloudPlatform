package org.nan.cloud.core.infrastructure.repository.mysql.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.nan.cloud.core.domain.Role;
import org.nan.cloud.core.infrastructure.repository.mysql.DO.RoleDO;
import org.nan.cloud.core.infrastructure.repository.mysql.converter.CommonConverter;
import org.nan.cloud.core.infrastructure.repository.mysql.mapper.RoleMapper;
import org.nan.cloud.core.repository.RoleRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

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
    public boolean allRolesExist(List<Long> roles) {
        return roleMapper.selectCount(new LambdaQueryWrapper<RoleDO>()
                .eq(RoleDO::getRid, roles)) == roles.size();
    }
}
