package org.nan.cloud.file.api.dto;

import lombok.Data;
import lombok.Builder;
import io.swagger.v3.oas.annotations.media.Schema;

@Data
@Builder
@Schema(description = "文件夹创建响应")
public class FolderCreateResponse {
    @Schema(description = "文件夹ID")
    private String folderId;
    
    @Schema(description = "文件夹名")
    private String folderName;
    
    @Schema(description = "文件夹路径")
    private String folderPath;
    
    @Schema(description = "创建状态")
    private String status;
}