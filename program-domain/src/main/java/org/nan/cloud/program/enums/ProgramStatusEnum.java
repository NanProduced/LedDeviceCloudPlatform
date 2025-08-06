package org.nan.cloud.program.enums;

import lombok.Getter;

/**
 * 节目状态枚举
 * 对应用户需求中的节目状态管理
 */
@Getter
public enum ProgramStatusEnum {

    /**
     * 草稿状态 - 用户点击创建节目编辑了画布后，没有点击保存创建成新节目时的状态
     */
    DRAFT("草稿"),

    /**
     * 已发布状态 - 当前有设备上有这个节目（某个版本）
     */
    PUBLISHED("已发布"),

    /**
     * 待发布状态 - 通过审核但未发布到设备
     */
    PENDING("待发布"),

    /**
     * 模板状态 - 模板不能直接发布，可以在创建节目时加载模板来快速编辑节目
     */
    TEMPLATE("模板");

    private final String value;

    ProgramStatusEnum(String value) {
        this.value = value;
    }
}