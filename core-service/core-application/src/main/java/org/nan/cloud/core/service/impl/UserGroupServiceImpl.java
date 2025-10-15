package org.nan.cloud.core.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nan.cloud.common.basic.exception.BusinessRefuseException;
import org.nan.cloud.common.basic.exception.ExceptionEnum;
import org.nan.cloud.common.basic.utils.JsonUtils;
import org.nan.cloud.core.DTO.CreateUserGroupDTO;
import org.nan.cloud.core.DTO.UserGroupRelDTO;
import org.nan.cloud.core.domain.User;
import org.nan.cloud.core.domain.UserGroup;
import org.nan.cloud.core.enums.CacheType;
import org.nan.cloud.core.enums.UserGroupTypeEnum;
import org.nan.cloud.core.repository.UserGroupRepository;
import org.nan.cloud.core.repository.UserRepository;
import org.nan.cloud.core.service.BusinessCacheService;
import org.nan.cloud.core.service.CacheService;
import org.nan.cloud.core.service.UserGroupService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserGroupServiceImpl implements UserGroupService {

    private final UserGroupRepository userGroupRepository;
    private final UserRepository userRepository;
    private final CacheService cacheService;

    @Override
    public UserGroup getUserGroupById(Long ugid) {
        return userGroupRepository.getUserGroupById(ugid);
    }
    
    /**
     * 带缓存的用户组查询（需要组织ID进行缓存隔离）
     */
    @Override
    public UserGroup getUserGroupById(Long oid, Long ugid) {
        String cacheKey = CacheType.USER_GROUPS.buildOrgKey(oid, ugid.toString());
        
        return cacheService.get(cacheKey, () -> userGroupRepository.getUserGroupById(ugid), UserGroup.class);
    }

    @Override
    @Transactional
    public void createUserGroup(CreateUserGroupDTO dto, Long orgId) {
        // 1. 创建用户组
        UserGroup parentGroup = userGroupRepository.getUserGroupById(dto.getParentUgid());
        UserGroup newUserGroup = userGroupRepository.createUserGroup(UserGroup.builder()
                .oid(dto.getOid())
                .parent(dto.getParentUgid())
                .name(dto.getUgName())
                .description(dto.getDescription())
                .path(parentGroup.getPath())
                .ugType(UserGroupTypeEnum.NORMAL_GROUP.getType())
                .creatorId(dto.getCreatorUid())
                .build());
        
        // 2. 将新创建的用户组添加到缓存
        String cacheKey = CacheType.USER_GROUPS.buildOrgKey(orgId, newUserGroup.getUgid().toString());
        cacheService.put(cacheKey, newUserGroup, CacheType.USER_GROUPS.getDefaultTtl());
        
        log.info("用户组创建: orgId={}, name={}, creatorId={}, ugid={}", orgId, dto.getUgName(), dto.getCreatorUid(), newUserGroup.getUgid());
    }
    

    @Override
    @Transactional
    public void deleteUserGroup(Long oid, Long ugid, Long orgId) {
        // 1. 验证和删除用户组
        List<Long> allUserGroups = userGroupRepository.getAllUgidsByParent(ugid);
        List<User> users = userRepository.getUsersByUgids(oid, allUserGroups);
        if (!CollectionUtils.isEmpty(users)) {
            throw new BusinessRefuseException(ExceptionEnum.HAS_USER_IN_TARGET_GROUP,
                    "refuse to delete user group",
                    JsonUtils.toJson(users.stream().map(User::getUsername).toList()));
        }
        
        // 2. 删除用户组前先清理缓存
        for (Long deletedUgid : allUserGroups) {
            String cacheKey = CacheType.USER_GROUPS.buildOrgKey(orgId, deletedUgid.toString());
            cacheService.evict(cacheKey);
        }
        
        // 3. 删除用户组
        userGroupRepository.deleteUserGroupsByUgids(allUserGroups);
        
        log.info("用户组删除: orgId={}, ugid={}, deletedCount={}", orgId, ugid, allUserGroups.size());
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
