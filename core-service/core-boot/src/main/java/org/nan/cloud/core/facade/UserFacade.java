package org.nan.cloud.core.facade;

import lombok.RequiredArgsConstructor;
import org.nan.cloud.common.basic.utils.PasswordUtils;
import org.nan.cloud.core.service.UserService;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserFacade {

    private final UserService userService;

    public void modifyPassword(String oldPassword, String newPassword) {
        String encode = PasswordUtils.encodeByBCrypt(newPassword);
    }
}
