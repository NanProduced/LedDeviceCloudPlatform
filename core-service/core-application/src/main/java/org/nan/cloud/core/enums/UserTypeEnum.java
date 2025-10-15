package org.nan.cloud.core.enums;

import lombok.Getter;

@Getter
public enum UserTypeEnum {

    SYSTEM_USER(0),

    ORG_MANAGER_USER(1),

    NORMAL_USER(2);

    private final Integer code;

    UserTypeEnum(Integer code) {
        this.code = code;
    }
}
