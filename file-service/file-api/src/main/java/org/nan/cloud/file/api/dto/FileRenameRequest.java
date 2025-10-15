package org.nan.cloud.file.api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;

@Data
@Schema(description = "文件重命名请求")
public class FileRenameRequest {
    @NotBlank(message = "文件ID不能为空")
    @Schema(description = "文件ID", required = true)
    private String fileId;
    
    @NotBlank(message = "新文件名不能为空")
    @Schema(description = "新文件名", required = true)
    private String newFilename;
}