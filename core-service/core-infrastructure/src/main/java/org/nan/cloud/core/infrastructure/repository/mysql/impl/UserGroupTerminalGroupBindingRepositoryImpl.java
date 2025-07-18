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

import java.util.List;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class UserGroupTerminalGroupBindingRepositoryImpl implements UserGroupTerminalGroupBindingRepository {

    private final UserGroupTerminalGroupBindingMapper bindingMapper;
    private final CommonConverter commonConverter;

    @Override
    public void createBinding(UserGroupTerminalGroupBinding binding) {
        UserGroupTerminalGroupBindingDO bindingDO = commonConverter.toUserGroupTerminalGroupBindingDO(binding);
        bindingMapper.insert(bindingDO);
    }

    @Override
    public void deleteBinding(Long tgid, Long ugid) {
        LambdaQueryWrapper<UserGroupTerminalGroupBindingDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserGroupTerminalGroupBindingDO::getTgid, tgid)
               .eq(UserGroupTerminalGroupBindingDO::getUgid, ugid);
        bindingMapper.delete(wrapper);
    }


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
}