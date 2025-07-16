package org.nan.cloud.core.service.impl;

import lombok.RequiredArgsConstructor;
import org.nan.cloud.common.basic.exception.BusinessRefuseException;
import org.nan.cloud.common.basic.exception.ExceptionEnum;
import org.nan.cloud.common.basic.utils.JsonUtils;
import org.nan.cloud.core.DTO.CreateUserGroupDTO;
import org.nan.cloud.core.DTO.UserGroupRelDTO;
import org.nan.cloud.core.domain.User;
import org.nan.cloud.core.domain.UserGroup;
import org.nan.cloud.core.enums.UserGroupTypeEnum;
import org.nan.cloud.core.repository.UserGroupRepository;
import org.nan.cloud.core.repository.UserRepository;
import org.nan.cloud.core.service.UserGroupService;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserGroupServiceImpl implements UserGroupService {

    private final UserGroupRepository userGroupRepository;

    private final UserRepository userRepository;

    @Override
    public UserGroup getUserGroupById(Long ugid) {
        return userGroupRepository.getUserGroupById(ugid);
    }

    @Override
    public void createUserGroup(CreateUserGroupDTO dto) {
        UserGroup parentGroup = userGroupRepository.getUserGroupById(dto.getParentUgid());
        userGroupRepository.createUserGroup(UserGroup.builder()
                .oid(dto.getOid())
                .parent(dto.getParentUgid())
                .name(dto.getUgName())
                .description(dto.getDescription())
                .path(parentGroup.getPath())
                .ugType(UserGroupTypeEnum.NORMAL_GROUP.getType())
                .creatorId(dto.getCreatorUid())
                .build());
    }

    @Override
    public void deleteUserGroup(Long oid, Long ugid) {
        List<Long> allUserGroups = userGroupRepository.getAllUgidsByParent(ugid);
        List<User> users = userRepository.getUsersByUgids(oid, allUserGroups);
        if (!CollectionUtils.isEmpty(users)) {
            throw new BusinessRefuseException(ExceptionEnum.HAS_USER_IN_TARGET_GROUP,
                    "refuse to delete user group",
                    JsonUtils.toJson(users.stream().map(User::getUsername).toList()));
        }
        userGroupRepository.deleteUserGroupsByUgids(allUserGroups);
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
