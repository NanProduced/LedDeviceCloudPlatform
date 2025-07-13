package org.nan.cloud.core.api;

import org.nan.cloud.core.api.DTO.req.CreateUserRequest;
import org.nan.cloud.core.api.DTO.req.ModifyUserPasswordRequest;
import org.nan.cloud.core.api.DTO.req.MoveUserRequest;
import org.nan.cloud.core.api.DTO.res.UserInfoResponse;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

public interface UserApi {

    String prefix = "/user";

    @GetMapping(prefix + "/current")
    UserInfoResponse getCurrentUserInfo();

    @PostMapping(prefix + "/modify/pwd")
    void modifyPassword(@Validated @RequestBody ModifyUserPasswordRequest  modifyUserPasswordRequest);

    @PostMapping(prefix + "/create")
    void createUser(@Validated @RequestBody CreateUserRequest createUserRequest);

    @PostMapping(prefix + "/inactive")
    void inactiveUser(@RequestParam("uid") Long uid);

    @PostMapping(prefix + "/move")
    void moveUser(@Validated @RequestBody MoveUserRequest moveUserRequest);

}
