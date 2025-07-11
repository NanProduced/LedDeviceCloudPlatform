package org.nan.cloud.core.service.impl;

import lombok.RequiredArgsConstructor;
import org.nan.cloud.core.domain.UserGroup;
import org.nan.cloud.core.repository.UserGroupRepository;
import org.nan.cloud.core.service.UserGroupService;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserGroupServiceImpl implements UserGroupService {

    private final UserGroupRepository userGroupRepository;

    @Override
    public UserGroup getUserGroupById(Long ugid) {
        return userGroupRepository.getUserGroupById(ugid);
    }
}
