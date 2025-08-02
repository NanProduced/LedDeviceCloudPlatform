package org.nan.cloud.core.api.DTO.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "创建文件夹请求")
@Data
public class CreateFolderRequest {

    @Schema(description = "用户组Id（未选择父级文件夹时，在用户组根目录下创建文件夹）")
    private Long ugid;

    @Schema(description = "父级文件夹Id（未选择用户组时，在目标父级文件夹下创建文件夹）")
    private Long fid;

    @Schema(description = "文件夹名称")
    @NotNull
    private String folderName;

    @Schema(description = "文件夹描述")
    private String description;
}
