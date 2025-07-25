package org.nan.cloud.file.api.dto;

import lombok.Data;
import lombok.Builder;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Data
@Builder
@Schema(description = "文件夹树响应")
public class FolderTreeResponse {
    @Schema(description = "文件夹树节点")
    private List<FolderNode> nodes;
    
    @Data
    @Builder
    public static class FolderNode {
        @Schema(description = "文件夹ID")
        private String folderId;
        
        @Schema(description = "文件夹名")
        private String folderName;
        
        @Schema(description = "文件夹路径")
        private String folderPath;
        
        @Schema(description = "子文件夹")
        private List<FolderNode> children;
    }
}