package org.nan.cloud.core.api;

import jakarta.validation.constraints.NotBlank;

public interface UserApi {

    String prefix = "/user";

    void modifyPassword(@NotBlank String oldPassword, @NotBlank String newPassword);

}
