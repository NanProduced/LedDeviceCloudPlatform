package org.nan.cloud.core.api.DTO.res;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CreateOrgResponse {

    private Long oid;

    private String orgName;

    private Integer suffix;

    private Long uid;

    private String username;

    private String password;

}
