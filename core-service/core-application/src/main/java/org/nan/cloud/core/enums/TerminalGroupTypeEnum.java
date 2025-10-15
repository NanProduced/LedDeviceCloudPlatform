package org.nan.cloud.core.enums;

import lombok.Getter;

@Getter
public enum TerminalGroupTypeEnum {

    ORG_ROOT_GROUP(0),

    NORMAL_GROUP(1);

    private final Integer type;

    TerminalGroupTypeEnum(Integer type) {
        this.type = type;
    }
}
