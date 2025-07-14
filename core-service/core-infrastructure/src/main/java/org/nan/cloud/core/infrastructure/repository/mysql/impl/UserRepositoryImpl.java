package org.nan.cloud.core.infrastructure.repository.mysql.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import org.nan.cloud.core.domain.User;
import org.nan.cloud.core.infrastructure.repository.mysql.DO.UserDO;
import org.nan.cloud.core.infrastructure.repository.mysql.converter.CommonConverter;
import org.nan.cloud.core.infrastructure.repository.mysql.mapper.UserMapper;
import org.nan.cloud.core.repository.UserRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

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
}
