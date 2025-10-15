package org.nan.cloud.core.enums;

import lombok.Getter;

@Getter
public enum UserStatusEnum {
    ACTIVE(0),

    INACTIVE(1);

    private final Integer code;

    UserStatusEnum(Integer code) {
        this.code = code;
    }
}
