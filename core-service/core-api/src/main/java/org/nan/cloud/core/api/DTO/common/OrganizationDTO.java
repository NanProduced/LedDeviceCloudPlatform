package org.nan.cloud.core.api.DTO.common;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "组织信息DTO")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrganizationDTO {

    @Schema(description = "组织Id")
    private Long oid;

    @Schema(description = "组织名称")
    private String orgName;

    @Schema(description = "组织后缀")
    private Integer suffix;
}
