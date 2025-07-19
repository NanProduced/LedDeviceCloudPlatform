package org.nan.cloud.core.infrastructure.repository.mysql.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.nan.cloud.common.basic.model.PageVO;
import org.nan.cloud.core.domain.UserGroupTerminalGroupBinding;
import org.nan.cloud.core.infrastructure.repository.mysql.DO.UserGroupTerminalGroupBindingDO;
import org.nan.cloud.core.infrastructure.repository.mysql.converter.CommonConverter;
import org.nan.cloud.core.infrastructure.repository.mysql.mapper.UserGroupTerminalGroupBindingMapper;
import org.nan.cloud.core.repository.UserGroupTerminalGroupBindingRepository;
import org.springframework.stereotype.Repository;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class UserGroupTerminalGroupBindingRepositoryImpl implements UserGroupTerminalGroupBindingRepository {

    private final UserGroupTerminalGroupBindingMapper bindingMapper;
    private final CommonConverter commonConverter;


    @Override
    public List<UserGroupTerminalGroupBinding> getUserGroupBindings(Long ugid) {
        return commonConverter.toUserGroupTerminalGroupBinding(bindingMapper.selectList(new LambdaQueryWrapper<UserGroupTerminalGroupBindingDO>()
                .eq(UserGroupTerminalGroupBindingDO::getUgid, ugid)));
    }


    @Override
    public boolean hasTerminalGroupPermission(Long ugid, Long tgid) {
        return bindingMapper.hasTerminalGroupPermission(ugid, tgid);
    }

    @Override
    public List<Long> getAccessibleTerminalGroupIds(Long ugid) {
        return bindingMapper.selectAccessibleTerminalGroupIds(ugid);
    }

    @Override
    public void replaceUserGroupPermissions(Long ugid, List<UserGroupTerminalGroupBinding> newBindings) {
        // 使用事务保证原子性
        // 1. 删除现有绑定
        deleteUserGroupBindings(ugid);
        
        // 2. 批量插入新绑定
        if (!CollectionUtils.isEmpty(newBindings)) {
            batchCreateBindings(newBindings);
        }
    }

    @Override
    public void deleteUserGroupBindings(Long ugid) {
        LambdaQueryWrapper<UserGroupTerminalGroupBindingDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserGroupTerminalGroupBindingDO::getUgid, ugid);
        bindingMapper.delete(wrapper);
    }

    @Override
    public void batchCreateBindings(List<UserGroupTerminalGroupBinding> bindings) {
        if (CollectionUtils.isEmpty(bindings)) return;

        
        List<UserGroupTerminalGroupBindingDO> bindingDOs = bindings.stream()
                .map(commonConverter::toUserGroupTerminalGroupBindingDO)
                .collect(Collectors.toList());
        
        // 使用自定义的批量插入方法
        bindingMapper.insertBatchSomeColumn(bindingDOs);
    }

    @Override
    public List<UserGroupTerminalGroupBinding> getUserGroupPermissionDetails(Long ugid) {
        // 调用Mapper的联表查询方法获取详细信息
        List<UserGroupTerminalGroupBindingDO> bindingDOs = bindingMapper.selectUserGroupPermissionDetails(ugid);
        return commonConverter.toUserGroupTerminalGroupBinding(bindingDOs);
    }
}