package org.nan.cloud.core.infrastructure.repository.mysql.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.nan.cloud.common.basic.model.BindingType;
import org.nan.cloud.common.basic.model.PageVO;
import org.nan.cloud.core.domain.UserGroupTerminalGroupBinding;
import org.nan.cloud.core.infrastructure.repository.mysql.DO.UserGroupTerminalGroupBindingDO;
import org.nan.cloud.core.infrastructure.repository.mysql.converter.CommonConverter;
import org.nan.cloud.core.infrastructure.repository.mysql.mapper.UserGroupTerminalGroupBindingMapper;
import org.nan.cloud.core.repository.UserGroupTerminalGroupBindingRepository;
import org.springframework.stereotype.Repository;
import org.springframework.util.CollectionUtils;

import java.util.*;
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
        // 使用优化后的CTE查询，精确计算INCLUDE/EXCLUDE权限
        return bindingMapper.hasTerminalGroupPermission(ugid, tgid);
    }
    
    @Override
    public Map<Long, Boolean> batchCheckPermissions(Long ugid, List<Long> tgids) {
        if (CollectionUtils.isEmpty(tgids)) {
            return Collections.emptyMap();
        }
        
        // 使用批量查询优化性能
        List<UserGroupTerminalGroupBindingMapper.TerminalGroupPermissionResult> results = 
                bindingMapper.batchCheckPermissions(ugid, tgids);
        
        return results.stream()
                .collect(Collectors.toMap(
                        UserGroupTerminalGroupBindingMapper.TerminalGroupPermissionResult::getTgid,
                        UserGroupTerminalGroupBindingMapper.TerminalGroupPermissionResult::getHasPermission
                ));
    }

    @Override
    public List<UserGroupTerminalGroupBinding> getRawUserGroupBindings(Long ugid) {
        return commonConverter.toUserGroupTerminalGroupBinding(bindingMapper.selectList(new LambdaQueryWrapper<UserGroupTerminalGroupBindingDO>()
                .eq(UserGroupTerminalGroupBindingDO::getUgid, ugid)
                .orderByAsc(UserGroupTerminalGroupBindingDO::getTgid)));
    }

    @Override
    public void replaceUserGroupPermissions(Long ugid, List<UserGroupTerminalGroupBinding> newBindings) {
        // 使用事务保证原子性：先删除后插入
        // 1. 删除现有绑定（支持混合INCLUDE/EXCLUDE类型）
        deleteUserGroupBindings(ugid);
        
        // 2. 批量插入新绑定（支持新的binding_type字段）
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
        
        // 转换为DO对象（包含binding_type字段）
        List<UserGroupTerminalGroupBindingDO> bindingDOs = bindings.stream()
                .map(commonConverter::toUserGroupTerminalGroupBindingDO)
                .collect(Collectors.toList());
        
        // 使用支持binding_type的批量插入方法
        bindingMapper.insertBatchSomeColumn(bindingDOs);
    }


    @Override
    public List<UserGroupTerminalGroupBinding> getUserGroupPermissionDetails(Long ugid) {
        // 调用Mapper的联表查询方法获取详细信息（包含binding_type和终端组信息）
        List<UserGroupTerminalGroupBindingDO> bindingDOs = bindingMapper.selectUserGroupPermissionDetails(ugid);
        return commonConverter.toUserGroupTerminalGroupBinding(bindingDOs);
    }
}