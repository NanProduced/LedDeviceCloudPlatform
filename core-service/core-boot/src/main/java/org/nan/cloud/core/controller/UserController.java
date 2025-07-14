package org.nan.cloud.core.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

import org.nan.cloud.core.api.DTO.req.CreateUserRequest;
import org.nan.cloud.core.api.DTO.req.ModifyUserPasswordRequest;
import org.nan.cloud.core.api.DTO.req.MoveUserRequest;
import org.nan.cloud.core.api.DTO.res.UserInfoResponse;
import org.nan.cloud.core.api.UserApi;
import org.nan.cloud.core.facade.UserFacade;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "User(用户控制器)", description = "用户相关的所有操作")
@RestController
@RequiredArgsConstructor
public class UserController implements UserApi {

    private final UserFacade userFacade;

    @Operation(
            summary = "返回当前登录用户信息",
            description = "返回当前登录用户信息",
            tags = {"用户信息"}
    )
    @Override
    public UserInfoResponse getCurrentUserInfo() {
        return userFacade.getCurrentUserInfo();
    }

    @Operation(
            summary = "修改密码",
            description = "当前登录用户修改自己的密码",
            tags = {"用户信息"}
    )
    @Override
    public void modifyPassword(ModifyUserPasswordRequest modifyUserPasswordRequest) {
        userFacade.modifyPassword(modifyUserPasswordRequest.getOldPassword(),
                modifyUserPasswordRequest.getNewPassword());
    }

    @Operation(
            summary = "创建用户",
            description = "在指定用户组下创建用户",
            tags = {"用户管理"}
    )
    @Override
    public void createUser(CreateUserRequest createUserRequest) {
        userFacade.createUser(createUserRequest);
    }

    @Operation(
            summary = "封禁用户",
            description = "封禁指定用户",
            tags = {"用户管理"}
    )
    @Override
    public void inactiveUser(Long uid) {
        userFacade.inactiveUser(uid);
    }

    @Operation(
            summary = "解封用户",
            description = "解封指定用户",
            tags = {"用户管理"}
    )
    @Override
    public void activeUser(Long uid) {
        userFacade.activeUser(uid);
    }

    @Operation(
            summary = "移动用户",
            description = "将用户从一个组移动至另一个组",
            tags = {"用户管理"}
    )
    @Override
    public void moveUser(MoveUserRequest moveUserRequest) {
        userFacade.moveUser(moveUserRequest);
    }

    @Operation(
            summary = "删除用户",
            description = "删除用户",
            tags = {"用户管理"}
    )
    @Override
    public void deleteUser(Long uid) {

    }
}
