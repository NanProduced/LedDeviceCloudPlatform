package org.nan.cloud.core.infrastructure.repository.enums;

import lombok.Getter;
import org.nan.cloud.core.enums.SystemRolesEnum;

@Getter
public enum SystemRolesRelEnums {

    ROOT(1L, SystemRolesEnum.ROOT),

    ORG_MANAGER(2L, SystemRolesEnum.ORG_MANAGER),

    SYSTEM_MANAGER(3L, SystemRolesEnum.SYSTEM_MANAGER);

    /**
     * mysql role 中 rid
     */
    private Long rid;

    private SystemRolesEnum systemRolesEnum;

    private SystemRolesRelEnums(Long rid, SystemRolesEnum systemRolesEnum) {
        this.rid = rid;
        this.systemRolesEnum = systemRolesEnum;
    }
}
