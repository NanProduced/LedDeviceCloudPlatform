package org.nan.cloud.file.api.dto;

import lombok.Data;
import lombok.Builder;
import io.swagger.v3.oas.annotations.media.Schema;

@Data
@Builder
@Schema(description = "文件复制响应")
public class FileCopyResponse {
    @Schema(description = "原文件ID")
    private String originalFileId;
    
    @Schema(description = "新文件ID")
    private String newFileId;
    
    @Schema(description = "新文件名")
    private String newFileName;
    
    @Schema(description = "复制状态")
    private String status;
}