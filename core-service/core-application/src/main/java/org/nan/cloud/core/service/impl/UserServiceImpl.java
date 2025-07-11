package org.nan.cloud.core.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nan.cloud.core.DTO.CreateOrgDTO;
import org.nan.cloud.core.domain.User;
import org.nan.cloud.core.exception.BusinessException;
import org.nan.cloud.core.repository.UserRepository;
import org.nan.cloud.core.service.UserService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

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
                    .type(0)
                    .creatorId(1L)
                    .status(0)
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
}
