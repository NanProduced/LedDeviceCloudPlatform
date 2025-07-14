package org.nan.cloud.core.infrastructure.repository.mysql.converter;

import org.mapstruct.Mapper;
import org.mapstruct.NullValueCheckStrategy;
import org.nan.cloud.core.domain.*;
import org.nan.cloud.core.infrastructure.repository.mysql.DO.*;

import java.util.List;

@Mapper(componentModel = "spring", nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public interface CommonConverter {

    OrganizationDO organization2OrganizationDO(Organization organization);

    Organization organizationDO2Organization(OrganizationDO organizationDO);

    TerminalGroupDO terminalGroup2TerminalGroupDO(TerminalGroup terminalGroup);

    TerminalGroup terminalGroupDO2TerminalGroup(TerminalGroupDO terminalGroupDO);

    UserGroupDO userGroup2UserGroupDO(UserGroup userGroup);

    UserGroup userGroupDO2UserGroup(UserGroupDO userGroupDO);

    List<UserGroup> userGroupDO2UserGroup(List<UserGroupDO> userGroupDOS);

    UserDO user2UserDO(User user);

    User userDO2User(UserDO userDO);

    List<User> userDO2User(List<UserDO> userDOS);

    Permission permissionDO2Permission(PermissionDO permissionDO);

    List<Permission> permissionDO2Permission(List<PermissionDO> permissionDOS);

    RoleDO role2RoleDO(Role role);

    Role roleDO2Role(RoleDO roleDO);

    List<Role> roleDO2Role(List<RoleDO> roleDOS);
}
