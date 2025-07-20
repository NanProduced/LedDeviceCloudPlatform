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
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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

    @Override
    public List<UserGroup> getUserGroupsByOrgId(Long orgId) {
        LambdaQueryWrapper<UserGroupDO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserGroupDO::getOid, orgId);
        List<UserGroupDO> userGroupDOs = userGroupMapper.selectList(queryWrapper);
        return userGroupDOs.stream()
                .map(commonConverter::userGroupDO2UserGroup)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteUserGroupsByUgids(List<Long> ugids) {
        userGroupMapper.deleteByIds(ugids);
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
                .likeRight(UserGroupDO::getPath, getPathByUgid(ugid) + "|")
                .or()
                .eq(UserGroupDO::getUgid, ugid));
        return commonConverter.userGroupDO2UserGroup(userGroupDOS);
    }

    @Override
    public List<Long> getAllUgidsByParent(Long ugid) {
        return userGroupMapper.selectList(new LambdaQueryWrapper<UserGroupDO>()
                .select(UserGroupDO::getUgid)
                .likeRight(UserGroupDO::getPath, getPathByUgid(ugid) + "|")
                .or()
                .eq(UserGroupDO::getUgid, ugid)).stream()
                .map(UserGroupDO::getUgid).toList();
    }

    @Override
    public String getPathByUgid(Long ugid) {
        return userGroupMapper.selectOne(new LambdaQueryWrapper<UserGroupDO>()
                .select(UserGroupDO::getPath)
                .eq(UserGroupDO::getUgid, ugid)).getPath();
    }

    @Override
    public List<UserGroup> getDirectUserGroupsByParent(Long ugid) {
        List<UserGroupDO> userGroupDOS = userGroupMapper.selectList(new LambdaQueryWrapper<UserGroupDO>()
                .eq(UserGroupDO::getParent, ugid));
        return commonConverter.userGroupDO2UserGroup(userGroupDOS);
    }

    @Override
    public boolean ifTheSameOrg(Long oid, Long targetTgid) {
        return userGroupMapper.exists(new LambdaQueryWrapper<UserGroupDO>()
                .eq(UserGroupDO::getOid, oid)
                .eq(UserGroupDO::getUgid, targetTgid));
    }

    @Override
    public boolean isAncestor(Long aUgid, Long bUgid) {
        return userGroupMapper.isAncestor(aUgid, bUgid);
    }

    @Override
    public boolean isSibling(Long aUgid, Long bUgid) {
        return userGroupMapper.isSibling(aUgid, bUgid);
    }

    @Override
    public boolean isAncestorOrSibling(Long aUgid, Long bUgid) {
        return userGroupMapper.isAncestorOrSibling(aUgid, bUgid);
    }
}
