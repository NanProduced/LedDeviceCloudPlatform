package org.nan.cloud.core.infrastructure.repository.mysql.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.nan.cloud.common.basic.model.PageVO;
import org.nan.cloud.core.DTO.QueryUserListDTO;
import org.nan.cloud.core.domain.User;
import org.nan.cloud.core.infrastructure.repository.mysql.DO.UserDO;
import org.nan.cloud.core.infrastructure.repository.mysql.converter.CommonConverter;
import org.nan.cloud.core.infrastructure.repository.mysql.mapper.UserMapper;
import org.nan.cloud.core.repository.UserRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Repository
@RequiredArgsConstructor
public class UserRepositoryImpl implements UserRepository {

    private final UserMapper userMapper;

    private final CommonConverter commonConverter;

    @Override
    public User createUser(User user) {
        final UserDO userDO = commonConverter.user2UserDO(user);
        userDO.setCreateTime(LocalDateTime.now());
        userDO.setUpdateTime(LocalDateTime.now());
        userMapper.insert(userDO);
        return commonConverter.userDO2User(userDO);
    }

    @Override
    public User getUserById(Long uid) {
        UserDO userDO = userMapper.selectById(uid);
        return userDO == null ? null : commonConverter.userDO2User(userDO);
    }

    @Override
    public PageVO<User> pageUsers(int pageNum, int pageSize, Long oid, Set<Long> ugids, String usernameKeyword, String emailKeyword, Integer status) {
        Page<UserDO> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<UserDO> queryWrapper = new LambdaQueryWrapper<UserDO>()
                .select(UserDO.class, user -> !user.getColumn().equals("password"))
                .eq(UserDO::getOid, oid)
                .in(UserDO::getUgid, ugids);
        if (StringUtils.isNotBlank(usernameKeyword)) {
            queryWrapper.like(UserDO::getUsername, usernameKeyword);
        }
        if (StringUtils.isNotBlank(emailKeyword)) {
            queryWrapper.like(UserDO::getEmail, emailKeyword);
        }
        if (Objects.nonNull(status)) {
            queryWrapper.eq(UserDO::getStatus, status);
        }
        queryWrapper.orderByDesc(UserDO::getCreateTime);

        IPage pageResult = userMapper.selectPage(page, queryWrapper);

        PageVO<User> pageVO = PageVO.<User>builder()
                .pageNum((int) pageResult.getCurrent())
                .pageSize((int) pageResult.getSize())
                .total(pageResult.getTotal())
                .records(commonConverter.userDO2User(pageResult.getRecords()))
                .build();
        pageVO.calculate();
        return pageVO;
    }

    @Override
    public List<User> getUsersByUgids(Long oid, List<Long> ugids) {
        List<UserDO> userDOS = userMapper.selectList(new LambdaQueryWrapper<UserDO>()
                .eq(UserDO::getOid, oid)
                .in(UserDO::getUgid, ugids));
        return commonConverter.userDO2User(userDOS);
    }

    @Override
    public void updateUser(User user) {
        UserDO userDO = commonConverter.user2UserDO(user);
        userDO.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(userDO);
    }

    @Override
    public void updateUserStatus(Long uid, Integer status) {
        userMapper.update(new LambdaUpdateWrapper<UserDO>()
                .eq(UserDO::getUid,uid)
                .set(UserDO::getStatus,status)
                .set(UserDO::getUpdateTime,LocalDateTime.now()));
    }

    @Override
    public void modifyUserGroup(Long uid, Long ugid) {
        userMapper.update(new LambdaUpdateWrapper<UserDO>()
                .eq(UserDO::getUid,uid)
                .set(UserDO::getUgid,ugid)
                .set(UserDO::getUpdateTime,LocalDateTime.now()));
    }

    @Override
    public void deleteUser(Long uid) {
        userMapper.deleteById(uid);
    }

    @Override
    public boolean ifHasSameUsername(Long oid, String username) {
        return userMapper.exists(new LambdaQueryWrapper<UserDO>()
                .eq(UserDO::getOid, oid)
                .eq(UserDO::getUsername, username));
    }

    @Override
    public boolean isAncestorOrSiblingByUser(Long aUgid, Long bUgid) {
        return userMapper.isAncestorOrSiblingByUser(aUgid, bUgid);
    }

    @Override
    public boolean ifTheSameOrg(Long oid, Long targetUid) {
        return userMapper.exists(new LambdaQueryWrapper<UserDO>()
                .eq(UserDO::getOid, oid)
                .eq(UserDO::getUid, targetUid));
    }
}
