package org.nan.cloud.core.DTO;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CreateOrgVO {

    private Long oid;

    private String orgName;

    private Integer suffix;

    private Long uid;

    private String username;
}
