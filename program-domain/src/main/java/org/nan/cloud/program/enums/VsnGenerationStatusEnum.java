package org.nan.cloud.program.enums;

import lombok.Getter;

/**
 * VSN生成状态枚举
 * 用于跟踪VSN文件的生成状态
 */
@Getter
public enum VsnGenerationStatusEnum {

    /**
     * 待生成 - 节目创建后的初始状态，等待VSN生成
     */
    PENDING("待生成"),

    /**
     * 生成中 - VSN文件正在异步生成过程中
     */
    PROCESSING("生成中"),

    /**
     * 生成完成 - VSN文件生成成功，可用于设备播放
     */
    COMPLETED("生成完成"),

    /**
     * 生成失败 - VSN文件生成失败，需要重试或检查数据
     */
    FAILED("生成失败");

    private final String value;

    VsnGenerationStatusEnum(String value) {
        this.value = value;
    }
}