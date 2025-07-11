package org.nan.cloud.core.facade;

import lombok.RequiredArgsConstructor;
import org.nan.cloud.common.basic.utils.PasswordUtils;
import org.nan.cloud.common.web.context.GenericInvocationContext;
import org.nan.cloud.common.web.context.InvocationContextHolder;
import org.nan.cloud.core.api.DTO.res.UserInfoResponse;
import org.nan.cloud.core.service.UserService;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserFacade {

    private final UserService userService;

    public UserInfoResponse getCurrentUserInfo() {
        GenericInvocationContext context = InvocationContextHolder.getContext();
        // todo
        return null;

    }

    public void modifyPassword(String oldPassword, String newPassword) {
        String encode = PasswordUtils.encodeByBCrypt(newPassword);
    }
}
