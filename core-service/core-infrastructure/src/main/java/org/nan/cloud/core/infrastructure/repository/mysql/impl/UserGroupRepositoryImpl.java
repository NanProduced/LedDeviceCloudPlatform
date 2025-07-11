package org.nan.cloud.core.infrastructure.repository.mysql.impl;

import lombok.RequiredArgsConstructor;
import org.nan.cloud.core.domain.UserGroup;
import org.nan.cloud.core.infrastructure.repository.mysql.DO.UserGroupDO;
import org.nan.cloud.core.infrastructure.repository.mysql.converter.CommonConverter;
import org.nan.cloud.core.infrastructure.repository.mysql.mapper.UserGroupMapper;
import org.nan.cloud.core.repository.UserGroupRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
@RequiredArgsConstructor
public class UserGroupRepositoryImpl implements UserGroupRepository {

    private final UserGroupMapper userGroupMapper;

    private final CommonConverter commonConverter;

    @Override
    public UserGroup createUserGroup(UserGroup userGroup) {
        UserGroupDO userGroupDO = commonConverter.userGroup2UserGroupDO(userGroup);
        userGroupDO.setCreateTime(LocalDateTime.now());
        userGroupMapper.insert(userGroupDO);
        String path = userGroupDO.getPath().isBlank() ? String.valueOf(userGroupDO.getUgid()) : userGroupDO.getPath() + "|" + userGroupDO.getUgid();
        userGroupDO.setPath(path);
        userGroupMapper.updateById(userGroupDO);
        return commonConverter.userGroupDO2UserGroup(userGroupDO);
    }

    @Override
    public UserGroup getUserGroupById(Long ugid) {
        UserGroupDO userGroupDO = userGroupMapper.selectById(ugid);
        return userGroupDO == null ? null : commonConverter.userGroupDO2UserGroup(userGroupDO);
    }
}
