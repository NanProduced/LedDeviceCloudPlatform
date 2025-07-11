package org.nan.cloud.core.api;

import jakarta.validation.constraints.NotBlank;
import org.nan.cloud.core.api.DTO.res.UserInfoResponse;
import org.springframework.web.bind.annotation.GetMapping;

public interface UserApi {

    String prefix = "/user";

    @GetMapping(prefix + "/current")
    UserInfoResponse getCurrentUserInfo();

    void modifyPassword(@NotBlank String oldPassword, @NotBlank String newPassword);

}
