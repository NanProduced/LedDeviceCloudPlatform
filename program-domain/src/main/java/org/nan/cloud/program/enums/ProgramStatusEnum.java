package org.nan.cloud.program.enums;

import com.fasterxml.jackson.annotation.JsonValue;
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

    /* ------ 注：前端已适配后端响应值 ------------- */

    /**
     * 根据前端状态值获取枚举
     * 支持前端传入的状态字符串转换为后端枚举
     * 
     * @param frontendValue 前端状态值
     * @return 对应的枚举值
     */
    public static ProgramStatusEnum fromFrontendValue(String frontendValue) {
        if (frontendValue == null) return null;
        return switch(frontendValue.toLowerCase()) {
            case "draft" -> DRAFT;
            case "ready" -> PENDING;
            case "published" -> PUBLISHED;
            case "template" -> TEMPLATE;
            default -> throw new IllegalArgumentException("Unknown frontend status: " + frontendValue);
        };
    }

    /**
     * 前端兼容状态值
     * 将后端枚举转换为前端期望的状态字符串
     * 
     * @return 前端兼容的状态值
     */
    //@JsonValue
    public String getFrontendValue() {
        return switch(this) {
            case DRAFT -> "draft";
            case PENDING -> "ready";      // 前端使用"ready"表示待发布状态
            case PUBLISHED -> "published";
            case TEMPLATE -> "template";
        };
    }

}