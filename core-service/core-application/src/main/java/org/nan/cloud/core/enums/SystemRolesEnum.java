package org.nan.cloud.core.enums;

import lombok.Getter;

@Getter
public enum SystemRolesEnum {

    ROOT("root", "系统超级管理员"),

    SYSTEM_MANAGER("system_manager", "系统管理员，内部开发人员/支持/运维人员账号"),

    ORG_MANAGER("org_manager", "组织管理员，组织内最高权限用户，组织拥有者");

    private String name;

    private String description;

    private SystemRolesEnum(String name, String description) {
        this.name = name;
        this.description = description;
    }
}
