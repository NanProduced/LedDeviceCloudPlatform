package org.nan.cloud.file.api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;

@Data
@Schema(description = "文件移动请求")
public class FileMoveRequest {
    @NotBlank(message = "文件ID不能为空")
    @Schema(description = "文件ID", required = true)
    private String fileId;
    
    @NotBlank(message = "目标路径不能为空")
    @Schema(description = "目标路径", required = true)
    private String targetPath;
}