package org.nan.cloud.common.basic.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 转码详情统一模型（跨服务共享）
 *
 * 存储于 MongoDB 集合：material_transcode_detail
 * 不使用 @Document 注解，由各服务使用 MongoTemplate 进行读写。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TranscodingDetail {

    /**
     * MongoDB ObjectId 字符串
     */
    private String id;

    private String taskId;
    private Long oid;
    private Long uid;

    private Long sourceMaterialId;
    private String sourceFileId;
    private String targetFileId;

    /**
     * 预设名/代码（与 MySQL material.transcode_preset 对应）
     */
    private String presetName;

    /**
     * 自定义参数或最终生效的参数集合
     */
    private Map<String, Object> parameters;

    /**
     * 引擎信息
     */
    private EngineInfo engine;

    /**
     * 性能/时长等指标
     */
    private Metrics metrics;

    private String status; // PENDING|TRANSCODING|SAVING|COMPLETED|FAILED
    private String errorMessage;

    private LocalDateTime createdAt;
    private LocalDateTime completedAt;

    private String thumbnailPath;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EngineInfo {
        private String name;        // ffmpeg
        private String version;     // 4.4+
        private String commandLine; // 实际执行的命令
        private String hardware;    // cpu|nvidia
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Metrics {
        private Long durationMs;
        private Double avgFps;
        private Integer cpuPct;
        private Integer gpuPct;
        private Integer memMB;
    }
}

