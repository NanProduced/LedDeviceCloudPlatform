package org.nan.cloud.core.infrastructure.repository.mysql.converter;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.Generated;
import org.nan.cloud.core.domain.Organization;
import org.nan.cloud.core.domain.Permission;
import org.nan.cloud.core.domain.Role;
import org.nan.cloud.core.domain.TerminalGroup;
import org.nan.cloud.core.domain.User;
import org.nan.cloud.core.domain.UserGroup;
import org.nan.cloud.core.domain.UserGroupTerminalGroupBinding;
import org.nan.cloud.core.infrastructure.repository.mysql.DO.OrganizationDO;
import org.nan.cloud.core.infrastructure.repository.mysql.DO.PermissionDO;
import org.nan.cloud.core.infrastructure.repository.mysql.DO.RoleDO;
import org.nan.cloud.core.infrastructure.repository.mysql.DO.TerminalGroupDO;
import org.nan.cloud.core.infrastructure.repository.mysql.DO.UserDO;
import org.nan.cloud.core.infrastructure.repository.mysql.DO.UserGroupDO;
import org.nan.cloud.core.infrastructure.repository.mysql.DO.UserGroupTerminalGroupBindingDO;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-07-20T02:34:57+0800",
    comments = "version: 1.6.3, compiler: javac, environment: Java 17.0.15 (Eclipse Adoptium)"
)
@Component
public class CommonConverterImpl implements CommonConverter {

    @Override
    public OrganizationDO organization2OrganizationDO(Organization organization) {
        if ( organization == null ) {
            return null;
        }

        OrganizationDO organizationDO = new OrganizationDO();

        if ( organization.getOid() != null ) {
            organizationDO.setOid( organization.getOid() );
        }
        if ( organization.getName() != null ) {
            organizationDO.setName( organization.getName() );
        }
        if ( organization.getRemark() != null ) {
            organizationDO.setRemark( organization.getRemark() );
        }
        if ( organization.getRootTgid() != null ) {
            organizationDO.setRootTgid( organization.getRootTgid() );
        }
        if ( organization.getRootUgid() != null ) {
            organizationDO.setRootUgid( organization.getRootUgid() );
        }
        if ( organization.getSuffix() != null ) {
            organizationDO.setSuffix( organization.getSuffix() );
        }
        if ( organization.getCreateTime() != null ) {
            organizationDO.setCreateTime( organization.getCreateTime() );
        }
        if ( organization.getUpdateTime() != null ) {
            organizationDO.setUpdateTime( organization.getUpdateTime() );
        }
        if ( organization.getCreatorId() != null ) {
            organizationDO.setCreatorId( organization.getCreatorId() );
        }

        return organizationDO;
    }

    @Override
    public Organization organizationDO2Organization(OrganizationDO organizationDO) {
        if ( organizationDO == null ) {
            return null;
        }

        Organization.OrganizationBuilder organization = Organization.builder();

        if ( organizationDO.getOid() != null ) {
            organization.oid( organizationDO.getOid() );
        }
        if ( organizationDO.getName() != null ) {
            organization.name( organizationDO.getName() );
        }
        if ( organizationDO.getRemark() != null ) {
            organization.remark( organizationDO.getRemark() );
        }
        if ( organizationDO.getRootUgid() != null ) {
            organization.rootUgid( organizationDO.getRootUgid() );
        }
        if ( organizationDO.getRootTgid() != null ) {
            organization.rootTgid( organizationDO.getRootTgid() );
        }
        if ( organizationDO.getCreatorId() != null ) {
            organization.creatorId( organizationDO.getCreatorId() );
        }
        if ( organizationDO.getSuffix() != null ) {
            organization.suffix( organizationDO.getSuffix() );
        }
        if ( organizationDO.getCreateTime() != null ) {
            organization.createTime( organizationDO.getCreateTime() );
        }
        if ( organizationDO.getUpdateTime() != null ) {
            organization.updateTime( organizationDO.getUpdateTime() );
        }

        return organization.build();
    }

    @Override
    public TerminalGroupDO terminalGroup2TerminalGroupDO(TerminalGroup terminalGroup) {
        if ( terminalGroup == null ) {
            return null;
        }

        TerminalGroupDO terminalGroupDO = new TerminalGroupDO();

        if ( terminalGroup.getTgid() != null ) {
            terminalGroupDO.setTgid( terminalGroup.getTgid() );
        }
        if ( terminalGroup.getName() != null ) {
            terminalGroupDO.setName( terminalGroup.getName() );
        }
        if ( terminalGroup.getOid() != null ) {
            terminalGroupDO.setOid( terminalGroup.getOid() );
        }
        if ( terminalGroup.getParent() != null ) {
            terminalGroupDO.setParent( terminalGroup.getParent() );
        }
        if ( terminalGroup.getParentTgid() != null ) {
            terminalGroupDO.setParentTgid( terminalGroup.getParentTgid() );
        }
        if ( terminalGroup.getPath() != null ) {
            terminalGroupDO.setPath( terminalGroup.getPath() );
        }
        if ( terminalGroup.getDepth() != null ) {
            terminalGroupDO.setDepth( terminalGroup.getDepth() );
        }
        if ( terminalGroup.getTgType() != null ) {
            terminalGroupDO.setTgType( terminalGroup.getTgType() );
        }
        if ( terminalGroup.getDescription() != null ) {
            terminalGroupDO.setDescription( terminalGroup.getDescription() );
        }
        if ( terminalGroup.getCreatorId() != null ) {
            terminalGroupDO.setCreatorId( terminalGroup.getCreatorId() );
        }
        if ( terminalGroup.getCreateTime() != null ) {
            terminalGroupDO.setCreateTime( terminalGroup.getCreateTime() );
        }
        if ( terminalGroup.getUpdaterId() != null ) {
            terminalGroupDO.setUpdaterId( terminalGroup.getUpdaterId() );
        }
        if ( terminalGroup.getUpdateTime() != null ) {
            terminalGroupDO.setUpdateTime( terminalGroup.getUpdateTime() );
        }

        return terminalGroupDO;
    }

    @Override
    public TerminalGroup terminalGroupDO2TerminalGroup(TerminalGroupDO terminalGroupDO) {
        if ( terminalGroupDO == null ) {
            return null;
        }

        TerminalGroup.TerminalGroupBuilder terminalGroup = TerminalGroup.builder();

        if ( terminalGroupDO.getTgid() != null ) {
            terminalGroup.tgid( terminalGroupDO.getTgid() );
        }
        if ( terminalGroupDO.getName() != null ) {
            terminalGroup.name( terminalGroupDO.getName() );
        }
        if ( terminalGroupDO.getOid() != null ) {
            terminalGroup.oid( terminalGroupDO.getOid() );
        }
        if ( terminalGroupDO.getParent() != null ) {
            terminalGroup.parent( terminalGroupDO.getParent() );
        }
        if ( terminalGroupDO.getParentTgid() != null ) {
            terminalGroup.parentTgid( terminalGroupDO.getParentTgid() );
        }
        if ( terminalGroupDO.getPath() != null ) {
            terminalGroup.path( terminalGroupDO.getPath() );
        }
        if ( terminalGroupDO.getDepth() != null ) {
            terminalGroup.depth( terminalGroupDO.getDepth() );
        }
        if ( terminalGroupDO.getDescription() != null ) {
            terminalGroup.description( terminalGroupDO.getDescription() );
        }
        if ( terminalGroupDO.getTgType() != null ) {
            terminalGroup.tgType( terminalGroupDO.getTgType() );
        }
        if ( terminalGroupDO.getCreatorId() != null ) {
            terminalGroup.creatorId( terminalGroupDO.getCreatorId() );
        }
        if ( terminalGroupDO.getCreateTime() != null ) {
            terminalGroup.createTime( terminalGroupDO.getCreateTime() );
        }
        if ( terminalGroupDO.getUpdaterId() != null ) {
            terminalGroup.updaterId( terminalGroupDO.getUpdaterId() );
        }
        if ( terminalGroupDO.getUpdateTime() != null ) {
            terminalGroup.updateTime( terminalGroupDO.getUpdateTime() );
        }

        return terminalGroup.build();
    }

    @Override
    public List<TerminalGroup> terminalGroupDO2TerminalGroup(List<TerminalGroupDO> terminalGroupDOS) {
        if ( terminalGroupDOS == null ) {
            return null;
        }

        List<TerminalGroup> list = new ArrayList<TerminalGroup>( terminalGroupDOS.size() );
        for ( TerminalGroupDO terminalGroupDO : terminalGroupDOS ) {
            list.add( terminalGroupDO2TerminalGroup( terminalGroupDO ) );
        }

        return list;
    }

    @Override
    public UserGroupDO userGroup2UserGroupDO(UserGroup userGroup) {
        if ( userGroup == null ) {
            return null;
        }

        UserGroupDO userGroupDO = new UserGroupDO();

        if ( userGroup.getUgid() != null ) {
            userGroupDO.setUgid( userGroup.getUgid() );
        }
        if ( userGroup.getName() != null ) {
            userGroupDO.setName( userGroup.getName() );
        }
        if ( userGroup.getOid() != null ) {
            userGroupDO.setOid( userGroup.getOid() );
        }
        if ( userGroup.getParent() != null ) {
            userGroupDO.setParent( userGroup.getParent() );
        }
        if ( userGroup.getPath() != null ) {
            userGroupDO.setPath( userGroup.getPath() );
        }
        if ( userGroup.getUgType() != null ) {
            userGroupDO.setUgType( userGroup.getUgType() );
        }
        if ( userGroup.getDescription() != null ) {
            userGroupDO.setDescription( userGroup.getDescription() );
        }
        if ( userGroup.getCreatorId() != null ) {
            userGroupDO.setCreatorId( userGroup.getCreatorId() );
        }
        if ( userGroup.getCreateTime() != null ) {
            userGroupDO.setCreateTime( userGroup.getCreateTime() );
        }

        return userGroupDO;
    }

    @Override
    public UserGroup userGroupDO2UserGroup(UserGroupDO userGroupDO) {
        if ( userGroupDO == null ) {
            return null;
        }

        UserGroup.UserGroupBuilder userGroup = UserGroup.builder();

        if ( userGroupDO.getUgid() != null ) {
            userGroup.ugid( userGroupDO.getUgid() );
        }
        if ( userGroupDO.getName() != null ) {
            userGroup.name( userGroupDO.getName() );
        }
        if ( userGroupDO.getOid() != null ) {
            userGroup.oid( userGroupDO.getOid() );
        }
        if ( userGroupDO.getParent() != null ) {
            userGroup.parent( userGroupDO.getParent() );
        }
        if ( userGroupDO.getPath() != null ) {
            userGroup.path( userGroupDO.getPath() );
        }
        if ( userGroupDO.getDescription() != null ) {
            userGroup.description( userGroupDO.getDescription() );
        }
        if ( userGroupDO.getUgType() != null ) {
            userGroup.ugType( userGroupDO.getUgType() );
        }
        if ( userGroupDO.getCreatorId() != null ) {
            userGroup.creatorId( userGroupDO.getCreatorId() );
        }
        if ( userGroupDO.getCreateTime() != null ) {
            userGroup.createTime( userGroupDO.getCreateTime() );
        }

        return userGroup.build();
    }

    @Override
    public List<UserGroup> userGroupDO2UserGroup(List<UserGroupDO> userGroupDOS) {
        if ( userGroupDOS == null ) {
            return null;
        }

        List<UserGroup> list = new ArrayList<UserGroup>( userGroupDOS.size() );
        for ( UserGroupDO userGroupDO : userGroupDOS ) {
            list.add( userGroupDO2UserGroup( userGroupDO ) );
        }

        return list;
    }

    @Override
    public UserDO user2UserDO(User user) {
        if ( user == null ) {
            return null;
        }

        UserDO userDO = new UserDO();

        if ( user.getUid() != null ) {
            userDO.setUid( user.getUid() );
        }
        if ( user.getUsername() != null ) {
            userDO.setUsername( user.getUsername() );
        }
        if ( user.getPassword() != null ) {
            userDO.setPassword( user.getPassword() );
        }
        if ( user.getUgid() != null ) {
            userDO.setUgid( user.getUgid() );
        }
        if ( user.getPhone() != null ) {
            userDO.setPhone( user.getPhone() );
        }
        if ( user.getEmail() != null ) {
            userDO.setEmail( user.getEmail() );
        }
        if ( user.getStatus() != null ) {
            userDO.setStatus( user.getStatus() );
        }
        if ( user.getUserType() != null ) {
            userDO.setUserType( user.getUserType() );
        }
        if ( user.getOid() != null ) {
            userDO.setOid( user.getOid() );
        }
        if ( user.getSuffix() != null ) {
            userDO.setSuffix( user.getSuffix().longValue() );
        }
        if ( user.getCreateTime() != null ) {
            userDO.setCreateTime( user.getCreateTime() );
        }
        if ( user.getCreatorId() != null ) {
            userDO.setCreatorId( user.getCreatorId() );
        }
        if ( user.getUpdateTime() != null ) {
            userDO.setUpdateTime( user.getUpdateTime() );
        }

        return userDO;
    }

    @Override
    public User userDO2User(UserDO userDO) {
        if ( userDO == null ) {
            return null;
        }

        User.UserBuilder user = User.builder();

        if ( userDO.getUid() != null ) {
            user.uid( userDO.getUid() );
        }
        if ( userDO.getUsername() != null ) {
            user.username( userDO.getUsername() );
        }
        if ( userDO.getPassword() != null ) {
            user.password( userDO.getPassword() );
        }
        if ( userDO.getUgid() != null ) {
            user.ugid( userDO.getUgid() );
        }
        if ( userDO.getOid() != null ) {
            user.oid( userDO.getOid() );
        }
        if ( userDO.getPhone() != null ) {
            user.phone( userDO.getPhone() );
        }
        if ( userDO.getEmail() != null ) {
            user.email( userDO.getEmail() );
        }
        if ( userDO.getStatus() != null ) {
            user.status( userDO.getStatus() );
        }
        if ( userDO.getUserType() != null ) {
            user.userType( userDO.getUserType() );
        }
        if ( userDO.getSuffix() != null ) {
            user.suffix( userDO.getSuffix().intValue() );
        }
        if ( userDO.getCreatorId() != null ) {
            user.creatorId( userDO.getCreatorId() );
        }
        if ( userDO.getUpdateTime() != null ) {
            user.updateTime( userDO.getUpdateTime() );
        }
        if ( userDO.getCreateTime() != null ) {
            user.createTime( userDO.getCreateTime() );
        }

        return user.build();
    }

    @Override
    public List<User> userDO2User(List<UserDO> userDOS) {
        if ( userDOS == null ) {
            return null;
        }

        List<User> list = new ArrayList<User>( userDOS.size() );
        for ( UserDO userDO : userDOS ) {
            list.add( userDO2User( userDO ) );
        }

        return list;
    }

    @Override
    public Permission permissionDO2Permission(PermissionDO permissionDO) {
        if ( permissionDO == null ) {
            return null;
        }

        Permission permission = new Permission();

        if ( permissionDO.getPermissionId() != null ) {
            permission.setPermissionId( permissionDO.getPermissionId() );
        }
        if ( permissionDO.getName() != null ) {
            permission.setName( permissionDO.getName() );
        }
        if ( permissionDO.getUrl() != null ) {
            permission.setUrl( permissionDO.getUrl() );
        }
        if ( permissionDO.getMethod() != null ) {
            permission.setMethod( permissionDO.getMethod() );
        }
        if ( permissionDO.getDescription() != null ) {
            permission.setDescription( permissionDO.getDescription() );
        }
        if ( permissionDO.getPermissionGroup() != null ) {
            permission.setPermissionGroup( permissionDO.getPermissionGroup() );
        }
        if ( permissionDO.getPermissionType() != null ) {
            permission.setPermissionType( permissionDO.getPermissionType() );
        }

        return permission;
    }

    @Override
    public List<Permission> permissionDO2Permission(List<PermissionDO> permissionDOS) {
        if ( permissionDOS == null ) {
            return null;
        }

        List<Permission> list = new ArrayList<Permission>( permissionDOS.size() );
        for ( PermissionDO permissionDO : permissionDOS ) {
            list.add( permissionDO2Permission( permissionDO ) );
        }

        return list;
    }

    @Override
    public RoleDO role2RoleDO(Role role) {
        if ( role == null ) {
            return null;
        }

        RoleDO roleDO = new RoleDO();

        if ( role.getRid() != null ) {
            roleDO.setRid( role.getRid() );
        }
        if ( role.getOid() != null ) {
            roleDO.setOid( role.getOid() );
        }
        if ( role.getName() != null ) {
            roleDO.setName( role.getName() );
        }
        if ( role.getDisplayName() != null ) {
            roleDO.setDisplayName( role.getDisplayName() );
        }
        if ( role.getDescription() != null ) {
            roleDO.setDescription( role.getDescription() );
        }
        if ( role.getRoleType() != null ) {
            roleDO.setRoleType( role.getRoleType() );
        }
        if ( role.getCreatorId() != null ) {
            roleDO.setCreatorId( role.getCreatorId() );
        }
        if ( role.getCreateTime() != null ) {
            roleDO.setCreateTime( role.getCreateTime() );
        }
        if ( role.getUpdaterId() != null ) {
            roleDO.setUpdaterId( role.getUpdaterId() );
        }
        if ( role.getUpdateTime() != null ) {
            roleDO.setUpdateTime( role.getUpdateTime() );
        }

        return roleDO;
    }

    @Override
    public Role roleDO2Role(RoleDO roleDO) {
        if ( roleDO == null ) {
            return null;
        }

        Role.RoleBuilder role = Role.builder();

        if ( roleDO.getRid() != null ) {
            role.rid( roleDO.getRid() );
        }
        if ( roleDO.getOid() != null ) {
            role.oid( roleDO.getOid() );
        }
        if ( roleDO.getName() != null ) {
            role.name( roleDO.getName() );
        }
        if ( roleDO.getDisplayName() != null ) {
            role.displayName( roleDO.getDisplayName() );
        }
        if ( roleDO.getDescription() != null ) {
            role.description( roleDO.getDescription() );
        }
        if ( roleDO.getRoleType() != null ) {
            role.roleType( roleDO.getRoleType() );
        }
        if ( roleDO.getCreatorId() != null ) {
            role.creatorId( roleDO.getCreatorId() );
        }
        if ( roleDO.getCreateTime() != null ) {
            role.createTime( roleDO.getCreateTime() );
        }
        if ( roleDO.getUpdaterId() != null ) {
            role.updaterId( roleDO.getUpdaterId() );
        }
        if ( roleDO.getUpdateTime() != null ) {
            role.updateTime( roleDO.getUpdateTime() );
        }

        return role.build();
    }

    @Override
    public List<Role> roleDO2Role(List<RoleDO> roleDOS) {
        if ( roleDOS == null ) {
            return null;
        }

        List<Role> list = new ArrayList<Role>( roleDOS.size() );
        for ( RoleDO roleDO : roleDOS ) {
            list.add( roleDO2Role( roleDO ) );
        }

        return list;
    }

    @Override
    public UserGroupTerminalGroupBindingDO toUserGroupTerminalGroupBindingDO(UserGroupTerminalGroupBinding binding) {
        if ( binding == null ) {
            return null;
        }

        UserGroupTerminalGroupBindingDO userGroupTerminalGroupBindingDO = new UserGroupTerminalGroupBindingDO();

        if ( binding.getBindingId() != null ) {
            userGroupTerminalGroupBindingDO.setBindingId( binding.getBindingId() );
        }
        if ( binding.getUgid() != null ) {
            userGroupTerminalGroupBindingDO.setUgid( binding.getUgid() );
        }
        if ( binding.getTgid() != null ) {
            userGroupTerminalGroupBindingDO.setTgid( binding.getTgid() );
        }
        if ( binding.getIncludeSub() != null ) {
            userGroupTerminalGroupBindingDO.setIncludeSub( binding.getIncludeSub() );
        }
        if ( binding.getBindingType() != null ) {
            userGroupTerminalGroupBindingDO.setBindingType( binding.getBindingType() );
        }
        if ( binding.getOid() != null ) {
            userGroupTerminalGroupBindingDO.setOid( binding.getOid() );
        }
        if ( binding.getCreatorId() != null ) {
            userGroupTerminalGroupBindingDO.setCreatorId( binding.getCreatorId() );
        }
        if ( binding.getCreateTime() != null ) {
            userGroupTerminalGroupBindingDO.setCreateTime( binding.getCreateTime() );
        }
        if ( binding.getUpdaterId() != null ) {
            userGroupTerminalGroupBindingDO.setUpdaterId( binding.getUpdaterId() );
        }
        if ( binding.getUpdateTime() != null ) {
            userGroupTerminalGroupBindingDO.setUpdateTime( binding.getUpdateTime() );
        }

        return userGroupTerminalGroupBindingDO;
    }

    @Override
    public UserGroupTerminalGroupBinding toUserGroupTerminalGroupBinding(UserGroupTerminalGroupBindingDO bindingDO) {
        if ( bindingDO == null ) {
            return null;
        }

        UserGroupTerminalGroupBinding.UserGroupTerminalGroupBindingBuilder userGroupTerminalGroupBinding = UserGroupTerminalGroupBinding.builder();

        if ( bindingDO.getBindingId() != null ) {
            userGroupTerminalGroupBinding.bindingId( bindingDO.getBindingId() );
        }
        if ( bindingDO.getUgid() != null ) {
            userGroupTerminalGroupBinding.ugid( bindingDO.getUgid() );
        }
        if ( bindingDO.getTgid() != null ) {
            userGroupTerminalGroupBinding.tgid( bindingDO.getTgid() );
        }
        if ( bindingDO.getIncludeSub() != null ) {
            userGroupTerminalGroupBinding.includeSub( bindingDO.getIncludeSub() );
        }
        if ( bindingDO.getBindingType() != null ) {
            userGroupTerminalGroupBinding.bindingType( bindingDO.getBindingType() );
        }
        if ( bindingDO.getOid() != null ) {
            userGroupTerminalGroupBinding.oid( bindingDO.getOid() );
        }
        if ( bindingDO.getCreatorId() != null ) {
            userGroupTerminalGroupBinding.creatorId( bindingDO.getCreatorId() );
        }
        if ( bindingDO.getCreateTime() != null ) {
            userGroupTerminalGroupBinding.createTime( bindingDO.getCreateTime() );
        }
        if ( bindingDO.getUpdaterId() != null ) {
            userGroupTerminalGroupBinding.updaterId( bindingDO.getUpdaterId() );
        }
        if ( bindingDO.getUpdateTime() != null ) {
            userGroupTerminalGroupBinding.updateTime( bindingDO.getUpdateTime() );
        }

        return userGroupTerminalGroupBinding.build();
    }

    @Override
    public List<UserGroupTerminalGroupBinding> toUserGroupTerminalGroupBinding(List<UserGroupTerminalGroupBindingDO> bindingDOs) {
        if ( bindingDOs == null ) {
            return null;
        }

        List<UserGroupTerminalGroupBinding> list = new ArrayList<UserGroupTerminalGroupBinding>( bindingDOs.size() );
        for ( UserGroupTerminalGroupBindingDO userGroupTerminalGroupBindingDO : bindingDOs ) {
            list.add( toUserGroupTerminalGroupBinding( userGroupTerminalGroupBindingDO ) );
        }

        return list;
    }
}
