package org.nan.cloud.core.infrastructure.repository.mysql.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.nan.cloud.core.domain.UserGroup;
import org.nan.cloud.core.infrastructure.repository.mysql.DO.UserGroupDO;
import org.nan.cloud.core.infrastructure.repository.mysql.converter.CommonConverter;
import org.nan.cloud.core.infrastructure.repository.mysql.mapper.UserGroupMapper;
import org.nan.cloud.core.repository.UserGroupRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

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

    /**
     * 数据量大于10w  or -> union
     * @param ugid
     * @return
     */
    @Override
    public List<UserGroup> getAllUserGroupsByParent(Long ugid) {
        List<UserGroupDO> userGroupDOS = userGroupMapper.selectList(new LambdaQueryWrapper<UserGroupDO>()
                .select(UserGroupDO::getUgid, UserGroupDO::getName, UserGroupDO::getParent, UserGroupDO::getPath)
                .likeRight(UserGroupDO::getPath, ugid + "|")
                .or()
                .eq(UserGroupDO::getUgid, ugid));
        return commonConverter.userGroupDO2UserGroup(userGroupDOS);
    }

    @Override
    public List<UserGroup> getDirectUserGroupsByParent(Long ugid) {
        List<UserGroupDO> userGroupDOS = userGroupMapper.selectList(new LambdaQueryWrapper<UserGroupDO>()
                .eq(UserGroupDO::getParent, ugid));
        return commonConverter.userGroupDO2UserGroup(userGroupDOS);
    }
}
