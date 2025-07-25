package org.nan.cloud.file.api.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Data
@Schema(description = "批量删除文件请求")
public class BatchFileDeleteRequest {
    @NotEmpty(message = "文件ID列表不能为空")
    @Schema(description = "文件ID列表", required = true)
    private List<String> fileIds;
    
    @Schema(description = "删除原因")
    private String reason;
}