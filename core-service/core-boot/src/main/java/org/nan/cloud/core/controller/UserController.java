package org.nan.cloud.core.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.nan.cloud.core.api.DTO.req.ModifyUserPasswordRequest;
import org.nan.cloud.core.api.DTO.res.UserInfoResponse;
import org.nan.cloud.core.api.UserApi;
import org.nan.cloud.core.facade.UserFacade;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "User(用户控制器)", description = "用户相关的所有操作")
@RestController
@RequiredArgsConstructor
public class UserController implements UserApi {

    private final UserFacade userFacade;

    @Override
    public UserInfoResponse getCurrentUserInfo() {
        return userFacade.getCurrentUserInfo();
    }

    @Override
    public void modifyPassword(ModifyUserPasswordRequest modifyUserPasswordRequest) {
        userFacade.modifyPassword(modifyUserPasswordRequest.getOldPassword(),
                modifyUserPasswordRequest.getNewPassword());
    }
}
