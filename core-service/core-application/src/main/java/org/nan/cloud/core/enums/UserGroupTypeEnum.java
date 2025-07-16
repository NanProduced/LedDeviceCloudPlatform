package org.nan.cloud.core.enums;

import lombok.Getter;

@Getter
public enum UserGroupTypeEnum {

    ORG_ROOT_GROUP(0),

    NORMAL_GROUP(1),;

    private final Integer type;

    UserGroupTypeEnum(Integer type) {
        this.type = type;
    }
}
