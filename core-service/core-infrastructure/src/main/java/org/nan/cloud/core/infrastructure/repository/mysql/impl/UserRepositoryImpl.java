package org.nan.cloud.core.infrastructure.repository.mysql.impl;

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
}
