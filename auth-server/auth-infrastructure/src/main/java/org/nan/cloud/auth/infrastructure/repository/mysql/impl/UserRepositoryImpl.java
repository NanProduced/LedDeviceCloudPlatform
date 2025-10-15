package org.nan.cloud.auth.infrastructure.repository.mysql.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.nan.cloud.auth.application.model.User;
import org.nan.cloud.auth.application.repository.UserRepository;
import org.nan.cloud.auth.infrastructure.repository.mysql.DO.UserDO;
import org.nan.cloud.auth.infrastructure.repository.mysql.UserConverter;
import org.nan.cloud.auth.infrastructure.repository.mysql.mapper.UserMapper;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class UserRepositoryImpl implements UserRepository {

    private final UserMapper userMapper;

    private final UserConverter userConverter;

    @Override
    public Optional<User> findUserByUsernameAndSuffix(String username, Long suffix) {
        final UserDO userDO = userMapper.selectOne(new LambdaQueryWrapper<>(UserDO.class)
                .eq(UserDO::getSuffix, suffix)
                .eq(UserDO::getUsername, username));
        return Optional.ofNullable(userConverter.userDO2User(userDO));
    }
}
