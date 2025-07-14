package org.nan.cloud.core.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nan.cloud.common.basic.exception.ExceptionEnum;
import org.nan.cloud.core.DTO.CreateOrgDTO;


import org.nan.cloud.core.DTO.CreateUserDTO;
import org.nan.cloud.core.domain.User;
import org.nan.cloud.core.enums.UserStatusEnum;
import org.nan.cloud.core.enums.UserTypeEnum;
import org.nan.cloud.core.exception.BusinessException;
import org.nan.cloud.core.repository.UserRepository;
import org.nan.cloud.core.service.UserService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Override
    public User createOrgManagerUser(CreateOrgDTO createOrgDTO) {
        User manager;
        try {
            manager = userRepository.createUser(User
                    .builder()
                    .username(createOrgDTO.getManagerName())
                    .password(createOrgDTO.getManagerPsw())
                    .ugid(createOrgDTO.getUgid())
                    .oid(createOrgDTO.getTgid())
                    .phone(createOrgDTO.getPhone())
                    .email(createOrgDTO.getEmail())
                    .type(UserTypeEnum.ORG_MANAGER_USER.getCode())
                    .creatorId(1L)
                    .status(UserStatusEnum.ACTIVE.getCode())
                    .suffix(createOrgDTO.getSuffix())
                    .build());
        } catch (DuplicateKeyException ex) {
            throw new BusinessException(ex, "Duplicate usernames within the organization");
        }
        return manager;
    }

    @Override
    public User getUserById(Long uid) {
        return userRepository.getUserById(uid);
    }

    @Override
    public Long createUser(CreateUserDTO createUserDTO) {
        ExceptionEnum.SAME_USERNAME.throwIf(userRepository.ifHasSameUsername(createUserDTO.getOid(), createUserDTO.getUsername()));
        User user = userRepository.createUser(User.builder()
                .username(createUserDTO.getUsername())
                .password(createUserDTO.getEncodePassword())
                .ugid(createUserDTO.getUgid())
                .oid(createUserDTO.getOid())
                .phone(createUserDTO.getPhone())
                .email(createUserDTO.getEmail())
                .type(UserTypeEnum.NORMAL_USER.getCode())
                .creatorId(createUserDTO.getCreatorId())
                .status(UserStatusEnum.ACTIVE.getCode())
                .suffix(createUserDTO.getSuffix())
                .build());
        return user.getUid();
    }

    @Override
    public void updateUser(User user) {
        userRepository.updateUser(user);
    }

    @Override
    public void inactiveUser(Long uid) {
        userRepository.updateUserStatus(uid, UserStatusEnum.INACTIVE.getCode());
    }

    @Override
    public void activeUser(Long uid) {
        userRepository.updateUserStatus(uid, UserStatusEnum.ACTIVE.getCode());
    }

    @Override
    public void moveUser(Long uid, Long targetUid) {
        userRepository.modifyUserGroup(uid, targetUid);
    }

    @Override
    public void deleteUser(Long uid) {
        userRepository.deleteUser(uid);
    }
}
