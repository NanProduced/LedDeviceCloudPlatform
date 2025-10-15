package org.nan.cloud.file.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 转码验证结果DTO
 * 
 * @author LedDeviceCloudPlatform Team
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "转码验证结果")
public class TranscodingValidationResult {

    @Schema(description = "验证是否通过", example = "true")
    private boolean valid;

    @Schema(description = "错误消息", example = "源文件格式不支持")
    private String errorMessage;

    @Schema(description = "错误代码", example = "UNSUPPORTED_FORMAT")
    private String errorCode;

    @Schema(description = "警告信息列表")
    private List<String> warnings;

    @Schema(description = "验证详情")
    private ValidationDetails details;

    /**
     * 创建成功的验证结果
     */
    public static TranscodingValidationResult success() {
        return TranscodingValidationResult.builder()
                .valid(true)
                .build();
    }

    /**
     * 创建成功的验证结果（带警告）
     */
    public static TranscodingValidationResult success(List<String> warnings) {
        return TranscodingValidationResult.builder()
                .valid(true)
                .warnings(warnings)
                .build();
    }

    /**
     * 创建失败的验证结果
     */
    public static TranscodingValidationResult failure(String errorMessage) {
        return TranscodingValidationResult.builder()
                .valid(false)
                .errorMessage(errorMessage)
                .build();
    }

    /**
     * 创建失败的验证结果（带错误代码）
     */
    public static TranscodingValidationResult failure(String errorMessage, String errorCode) {
        return TranscodingValidationResult.builder()
                .valid(false)
                .errorMessage(errorMessage)
                .errorCode(errorCode)
                .build();
    }

    /**
     * 验证详情
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "验证详情")
    public static class ValidationDetails {

        @Schema(description = "源文件是否存在", example = "true")
        private Boolean sourceFileExists;

        @Schema(description = "源文件格式是否支持", example = "true")
        private Boolean sourceFormatSupported;

        @Schema(description = "输出格式是否支持", example = "true")
        private Boolean outputFormatSupported;

        @Schema(description = "转码参数是否有效", example = "true")
        private Boolean parametersValid;

        @Schema(description = "GPU加速是否可用", example = "false")
        private Boolean gpuAccelerationAvailable;

        @Schema(description = "预估转码时间（分钟）", example = "15")
        private Integer estimatedTime;

        @Schema(description = "预估输出文件大小（字节）", example = "52428800")
        private Long estimatedOutputSize;

        @Schema(description = "预估资源使用")
        private ResourceUsageEstimate resourceUsage;
    }

    /**
     * 资源使用预估
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "资源使用预估")
    public static class ResourceUsageEstimate {

        @Schema(description = "预估CPU使用率(%)", example = "80")
        private Integer cpuUsage;

        @Schema(description = "预估内存使用(MB)", example = "2048")
        private Integer memoryUsage;

        @Schema(description = "预估磁盘空间(MB)", example = "1024")
        private Integer diskUsage;

        @Schema(description = "预估网络带宽(Mbps)", example = "10")
        private Integer networkBandwidth;
    }

    /**
     * 验证错误代码常量
     */
    public static class ErrorCode {
        public static final String SOURCE_FILE_NOT_FOUND = "SOURCE_FILE_NOT_FOUND";
        public static final String UNSUPPORTED_SOURCE_FORMAT = "UNSUPPORTED_SOURCE_FORMAT";
        public static final String UNSUPPORTED_OUTPUT_FORMAT = "UNSUPPORTED_OUTPUT_FORMAT";
        public static final String INVALID_PARAMETERS = "INVALID_PARAMETERS";
        public static final String INSUFFICIENT_RESOURCES = "INSUFFICIENT_RESOURCES";
        public static final String PERMISSION_DENIED = "PERMISSION_DENIED";
        public static final String FILE_TOO_LARGE = "FILE_TOO_LARGE";
        public static final String CORRUPTED_FILE = "CORRUPTED_FILE";
        public static final String SYSTEM_UNAVAILABLE = "SYSTEM_UNAVAILABLE";
    }
}