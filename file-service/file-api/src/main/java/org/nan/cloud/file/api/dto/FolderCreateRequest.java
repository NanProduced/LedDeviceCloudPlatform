package org.nan.cloud.file.api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;

@Data
@Schema(description = "文件夹创建请求")
public class FolderCreateRequest {
    @NotBlank(message = "文件夹名不能为空")
    @Schema(description = "文件夹名", required = true)
    private String folderName;
    
    @Schema(description = "父文件夹路径")
    private String parentPath;
    
    @Schema(description = "父文件夹ID")
    private String parentFolderId;
    
    @Schema(description = "组织ID")
    private String organizationId;
}