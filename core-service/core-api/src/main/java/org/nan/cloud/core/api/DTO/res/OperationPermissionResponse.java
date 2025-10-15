package org.nan.cloud.core.api.DTO.res;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Schema(description = "用户可用功能响应DTO")
@JsonInclude(JsonInclude.Include.NON_NULL)
@AllArgsConstructor
@NoArgsConstructor
public class OperationPermissionResponse {

    private Long operationPermissionId;

    private String operationName;

    private String operationDescription;

    private String operationType;
}
