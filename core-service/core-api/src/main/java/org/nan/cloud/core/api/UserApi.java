package org.nan.cloud.core.api;

import jakarta.validation.constraints.NotBlank;
import org.nan.cloud.core.api.DTO.res.UserInfoResponse;

public interface UserApi {

    String prefix = "/user";

    UserInfoResponse getCurrentUserInfo();

    void modifyPassword(@NotBlank String oldPassword, @NotBlank String newPassword);

}
