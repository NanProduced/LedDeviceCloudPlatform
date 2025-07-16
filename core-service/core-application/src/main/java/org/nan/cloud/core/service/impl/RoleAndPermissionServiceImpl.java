package org.nan.cloud.core.service.impl;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.nan.cloud.common.basic.exception.BaseException;
import org.nan.cloud.common.basic.exception.BusinessRefuseException;
import org.nan.cloud.common.basic.exception.ExceptionEnum;
import org.nan.cloud.common.basic.utils.JsonUtils;
import org.nan.cloud.core.DTO.UpdateRoleDTO;
import org.nan.cloud.core.domain.Permission;
import org.nan.cloud.core.domain.Role;
import org.nan.cloud.core.domain.User;
import org.nan.cloud.core.repository.PermissionRepository;
import org.nan.cloud.core.repository.RoleRepository;
import org.nan.cloud.core.service.RoleAndPermissionService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoleAndPermissionServiceImpl implements RoleAndPermissionService {

    private final RoleRepository roleRepository;

    private final PermissionRepository permissionRepository;

    @Override
    public Role createRole(Role role) {
        return roleRepository.createRole(role);
    }

    @Override
    public Role getRoleByRid(Long rid) {
        return roleRepository.getRoleByRid(rid);
    }

    @Override
    public List<Role> getRolesByUid(Long uid) {
        return roleRepository.getRolesByUid(uid);
    }

    public void updateRole(UpdateRoleDTO updateRoleDTO) {
        Role role = getRoleByRid(updateRoleDTO.getRid());
        role.setUpdaterId(updateRoleDTO.getUpdaterUid());
        if (StringUtils.isNotBlank(updateRoleDTO.getRoleName())) {
            role.setDisplayName(updateRoleDTO.getRoleName());
        }
        if (StringUtils.isNotBlank(updateRoleDTO.getDescription())) {
            role.setDescription(updateRoleDTO.getDescription());
        }
        try {
            roleRepository.updateRole(role);
        } catch (DuplicateKeyException e) {
            throw new BaseException(ExceptionEnum.NAME_DUPLICATE_ERROR, e.getMessage());
        }
    }

    @Override
    public void deleteRole(Long oid, Long rid) {
        List<Long> userWithOnlyRole = roleRepository.getUserWithOnlyRole(oid, rid);
        if (!CollectionUtils.isEmpty(userWithOnlyRole)) {
            throw new BusinessRefuseException(ExceptionEnum.HAS_USER_WITH_ONLY_ROLE,
                    "refuse to delete role",
                    JsonUtils.toJson(userWithOnlyRole));
        }
        roleRepository.deleteRole(rid);
    }

    @Override
    public void createRolePermissionRel(Long rid, Set<Long> permissionIds) {
        permissionRepository.insertRolePermissionRel(rid, permissionIds);
    }

    @Override
    public List<Permission> getPermissionsByIds(List<Long> permissionIds) {
        return permissionRepository.getPermissionsByIds(permissionIds);
    }

    @Override
    public List<Permission> getPermissionsByUid(Long oid, Long uid) {
        List<Long> rids = roleRepository.getRidsByUid(uid);
        return permissionRepository.getPermissionsByRids(oid, rids);
    }


    @Override
    public List<Long> getPermissionIdsByRid(Long rid) {
        return permissionRepository.getPermissionIdsByRid(rid);
    }

    @Override
    public List<Permission> getAllPermissions() {
        return permissionRepository.getAllPermissions();
    }

    @Override
    public Map<Long, List<Role>> getRolesByUids(List<Long> userIds) {
        Map<Long, List<Long>> userRoleMap = roleRepository.getRoleIdsByUserIds(userIds);
        Set<Long> roleSet = userRoleMap.values().stream().flatMap(List::stream)
                .collect(Collectors.toSet());
        Map<Long, Role> roleMap = roleRepository.getRolesByRids(roleSet).stream()
                .collect(Collectors.toMap(Role::getRid, Function.identity()));
        Map<Long, List<Role>> result = new HashMap<>(userRoleMap.size());
        userRoleMap.forEach((uid, rids) -> {
            List<Role> roles = rids.stream()
                    .map(roleMap::get)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            result.put(uid, roles);
        });
        return result;
    }

    @Override
    public List<Role> getVisibleRolesByUid(Long oid, Long uid) {
        List<Long> rids = roleRepository.getRidsByUid(uid);
        return roleRepository.getCoveredRolesByRids(rids, oid);
    }

    @Override
    public void assignPermissionsToRole(Role role, List<Permission> permissions) {

    }

    @Override
    public void assignRoleToUser(Role role, User user) {

    }
}
