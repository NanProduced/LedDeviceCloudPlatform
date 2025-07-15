package org.nan.cloud.core.api.DTO.res;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Schema(description = "权限查询响应DTO")
@JsonInclude(JsonInclude.Include.NON_NULL)
@AllArgsConstructor
@NoArgsConstructor
public class PermissionResponse {

    private Long permissionId;

    private String permissionName;

    private String permissionDescription;

    private String permissionType;
}
