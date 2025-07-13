package org.nan.cloud.core.service.impl;

import lombok.RequiredArgsConstructor;
import org.nan.cloud.core.DTO.UserGroupRelDTO;
import org.nan.cloud.core.domain.UserGroup;
import org.nan.cloud.core.repository.UserGroupRepository;
import org.nan.cloud.core.service.UserGroupService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserGroupServiceImpl implements UserGroupService {

    private final UserGroupRepository userGroupRepository;

    @Override
    public UserGroup getUserGroupById(Long ugid) {
        return userGroupRepository.getUserGroupById(ugid);
    }

    @Override
    public List<UserGroupRelDTO> getAllUserGroupsByParent(Long ugid) {
        List<UserGroup> userGroups = userGroupRepository.getAllUserGroupsByParent(ugid);

        Map<Long, String> userGroupNameMap = userGroups.stream()
                .collect(Collectors.toMap(
                        UserGroup::getUgid,
                        UserGroup::getName
                ));

        return userGroups.stream()
                .map(e -> {
                    Map<Long, String> pathMap = e.parsePathToIdList().stream()
                            .collect(Collectors.toMap(
                                    id -> id,
                                    id -> userGroupNameMap.getOrDefault(id, "unknown")
                            ));

                    return UserGroupRelDTO.builder()
                            .ugid(e.getUgid())
                            .ugName(e.getName())
                            .parent(e.getParent())
                            .path(e.getPath())
                            .pathMap(pathMap)
                            .build();
                })
                .toList();
    }
}
