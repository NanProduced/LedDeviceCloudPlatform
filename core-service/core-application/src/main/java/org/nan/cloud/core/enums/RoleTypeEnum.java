package org.nan.cloud.core.enums;

import lombok.Getter;

@Getter
public enum RoleTypeEnum {
    SYSTEM_ROLE(0),

    CUSTOM_ROLE(1);

    private final Integer type;

    RoleTypeEnum(Integer type) {
        this.type = type;
    }
}
