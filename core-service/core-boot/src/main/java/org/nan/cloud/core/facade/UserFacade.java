package org.nan.cloud.core.facade;

import lombok.RequiredArgsConstructor;
import org.nan.cloud.common.basic.exception.ExceptionEnum;
import org.nan.cloud.common.basic.utils.PasswordUtils;
import org.nan.cloud.common.web.context.GenericInvocationContext;
import org.nan.cloud.common.web.context.InvocationContextHolder;
import org.nan.cloud.common.web.context.RequestUserInfo;
import org.nan.cloud.core.api.DTO.res.UserInfoResponse;
import org.nan.cloud.core.service.OrgService;
import org.nan.cloud.core.service.UserGroupService;
import org.nan.cloud.core.service.UserService;
import org.nan.cloud.core.repository.OrgRepository;
import org.nan.cloud.core.repository.UserGroupRepository;
import org.nan.cloud.core.domain.User;
import org.nan.cloud.core.domain.Organization;
import org.nan.cloud.core.domain.UserGroup;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class UserFacade {

    private final UserService userService;

    private final OrgService orgService;

    private final UserGroupService userGroupService;


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
}
