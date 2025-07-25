package org.nan.cloud.file.api.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.Map;

/**
 * 文件批量操作请求
 * 
 * @author LedDeviceCloudPlatform Team
 * @since 1.0.0
 */
@Data
@Schema(description = "文件批量操作请求")
public class FileBatchOperationRequest {

    /**
     * 文件ID列表
     */
    @NotEmpty(message = "文件ID列表不能为空")
    @Schema(description = "文件ID列表", required = true)
    private List<String> fileIds;

    /**
     * 操作类型
     */
    @Schema(description = "操作类型", example = "DELETE", required = true)
    private String operation;

    /**
     * 操作参数
     */
    @Schema(description = "操作参数")
    private Map<String, Object> parameters;

    /**
     * 是否异步执行
     */
    @Schema(description = "是否异步执行")
    private Boolean async = true;

    /**
     * 操作原因/备注
     */
    @Schema(description = "操作原因")
    private String reason;
}