package org.nan.cloud.program.enums;

import lombok.Getter;

/**
 * 节目审核状态枚举
 * 对应用户需求中的可配置审核流程
 */
@Getter
public enum ProgramApprovalStatusEnum {

    /**
     * 待审核 - 节目创建后默认进入待审核状态（如果组织开启审核功能）
     */
    PENDING("待审核"),

    /**
     * 已通过 - 有权限的用户审核后才能进入节目列表等待发布
     */
    APPROVED("已通过"),

    /**
     * 已拒绝 - 审核未通过
     */
    REJECTED("已拒绝");

    private final String value;

    ProgramApprovalStatusEnum(String value) {
        this.value = value;
    }
}