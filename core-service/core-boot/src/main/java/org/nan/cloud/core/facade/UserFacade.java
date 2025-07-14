package org.nan.cloud.core.facade;

import lombok.RequiredArgsConstructor;
import org.nan.cloud.common.basic.exception.ExceptionEnum;
import org.nan.cloud.common.basic.utils.PasswordUtils;
import org.nan.cloud.common.web.context.GenericInvocationContext;
import org.nan.cloud.common.web.context.InvocationContextHolder;
import org.nan.cloud.common.web.context.RequestUserInfo;
import org.nan.cloud.core.DTO.CreateUserDTO;
import org.nan.cloud.core.api.DTO.req.CreateUserRequest;
import org.nan.cloud.core.api.DTO.req.MoveUserRequest;
import org.nan.cloud.core.api.DTO.res.UserInfoResponse;
import org.nan.cloud.core.aspect.SkipOrgManagerPermissionCheck;
import org.nan.cloud.core.event.UserDeleteEvent;
import org.nan.cloud.core.service.*;
import org.nan.cloud.core.domain.User;
import org.nan.cloud.core.domain.Organization;
import org.nan.cloud.core.domain.UserGroup;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class UserFacade {

    private final UserService userService;

    private final OrgService orgService;

    private final UserGroupService userGroupService;

    private final PermissionChecker permissionChecker;

    private final PermissionEventPublisher permissionEventPublisher;

    private final ApplicationEventPublisher applicationEventPublisher;


    public UserInfoResponse getCurrentUserInfo() {
        GenericInvocationContext context = InvocationContextHolder.getContext();
        RequestUserInfo requestUser = context.getRequestUser();
        Long uid = requestUser.getUid();
        Long oid = requestUser.getOid();
        Long ugid = requestUser.getUgid();

        User user = userService.getUserById(uid);

        UserInfoResponse resp = new UserInfoResponse();
        resp.setUid(user.getUid());
        resp.setUsername(user.getUsername());
        resp.setOid(user.getOid());
        resp.setEmail(user.getEmail());
        resp.setPhone(user.getPhone());
        resp.setUgid(user.getUgid());

        // 组织名称
        Organization org = orgService.getOrgById(oid);
        resp.setOrgName(org.getName());
        // 用户组名称
        UserGroup userGroup = userGroupService.getUserGroupById(ugid);
        resp.setUgName(userGroup.getName());

        return resp;
    }

    @Transactional(rollbackFor = Exception.class)
    public void modifyPassword(String oldRawPassword, String newRawPassword) {
        Long currentUId = InvocationContextHolder.getCurrentUId();
        User currentUser = userService.getUserById(currentUId);
        String oldEncodedPassword = currentUser.getPassword();
        ExceptionEnum.USER_PASSWORD_NOT_MATCH.throwIf(!PasswordUtils.matchesByBCrypt(oldRawPassword, oldEncodedPassword));
        String newEncoded = PasswordUtils.encodeByBCrypt(newRawPassword);
        currentUser.setPassword(newEncoded);
        userService.updateUser(currentUser);
    }

    @Transactional(rollbackFor = Exception.class)
    @SkipOrgManagerPermissionCheck
    public void createUser(CreateUserRequest createUserRequest) {
        RequestUserInfo requestUser = InvocationContextHolder.getContext().getRequestUser();
        Long oid = requestUser.getOid();
        Long currentUId = requestUser.getUid();
        Long curUgid = requestUser.getUgid();
        ExceptionEnum.USER_GROUP_PERMISSION_DENIED.throwIf(!permissionChecker.ifHasPermissionOnTargetUserGroup(curUgid, createUserRequest.getUgid()));
        ExceptionEnum.ROLE_DOES_NOT_EXIST.throwIf(!permissionChecker.ifRolesExist(createUserRequest.getRoles()));
        ExceptionEnum.USER_PERMISSION_DENIED.throwIf(!permissionChecker.ifHasPermissionOnTargetRoles(oid,
                currentUId, createUserRequest.getRoles()));
        CreateUserDTO createUserDTO = CreateUserDTO.builder()
                .oid(oid)
                .suffix(orgService.getSuffixById(oid))
                .ugid(createUserRequest.getUgid())
                .username(createUserRequest.getUsername())
                .encodePassword(PasswordUtils.encodeByBCrypt(createUserRequest.getPassword()))
                .email(createUserRequest.getEmail())
                .phone(createUserRequest.getPhone())
                .creatorId(currentUId)
                .build();
        Long createUid = userService.createUser(createUserDTO);
        permissionEventPublisher.publishAddUserAndRoleRelEvent(createUid, oid, createUserRequest.getRoles());
    }

    @Transactional(rollbackFor = Exception.class)
    public void inactiveUser(Long uid) {
        RequestUserInfo requestUser = InvocationContextHolder.getContext().getRequestUser();
        ExceptionEnum.USER_PERMISSION_DENIED.throwIf(!permissionChecker.ifHasPermissionOnTargetUser(requestUser.getUid(), uid));
        userService.inactiveUser(uid);
    }

    @Transactional(rollbackFor = Exception.class)
    public void activeUser(Long uid) {
        RequestUserInfo requestUser = InvocationContextHolder.getContext().getRequestUser();
        ExceptionEnum.USER_PERMISSION_DENIED.throwIf(!permissionChecker.ifHasPermissionOnTargetUser(requestUser.getUid(), uid));
        userService.activeUser(uid);
    }

    @Transactional(rollbackFor = Exception.class)
    public void moveUser(MoveUserRequest moveUserRequest) {
        RequestUserInfo requestUser = InvocationContextHolder.getContext().getRequestUser();
        ExceptionEnum.USER_PERMISSION_DENIED.throwIf(!permissionChecker.ifHasPermissionOnTargetUser(requestUser.getUid(), moveUserRequest.getUid()));
        ExceptionEnum.USER_GROUP_PERMISSION_DENIED.throwIf(!permissionChecker.ifHasPermissionOnTargetUserGroup(requestUser.getUgid(), moveUserRequest.getTargetUgid()));
        userService.moveUser(moveUserRequest.getUid(), moveUserRequest.getTargetUgid());
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteUser(Long uid) {
        Long currenUid = InvocationContextHolder.getCurrentUId();
        ExceptionEnum.USER_PERMISSION_DENIED.throwIf(!permissionChecker.ifHasPermissionOnTargetUser(currenUid, uid));
        User deleteUser = userService.getUserById(uid);
        userService.deleteUser(uid);
        UserDeleteEvent event = new UserDeleteEvent(this, deleteUser, currenUid);
        applicationEventPublisher.publishEvent(event);
        permissionEventPublisher.publishRemoveUserAndRoleRelEvent(event);

    }
}
