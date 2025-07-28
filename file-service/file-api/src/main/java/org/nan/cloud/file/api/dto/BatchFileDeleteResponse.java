package org.nan.cloud.file.api.dto;

import lombok.Data;
import lombok.Builder;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Data
@Builder
@Schema(description = "批量删除文件响应")
public class BatchFileDeleteResponse {
    @Schema(description = "成功删除的文件ID")
    private List<String> successFiles;
    
    @Schema(description = "失败删除的文件ID")
    private List<String> failedFiles;
    
    @Schema(description = "成功数量")
    private Integer successCount;
    
    @Schema(description = "失败数量")
    private Integer failedCount;
    
    @Schema(description = "总文件数量")
    private Integer totalFiles;
}