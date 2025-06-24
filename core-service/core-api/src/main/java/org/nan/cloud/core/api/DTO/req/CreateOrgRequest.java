package org.nan.cloud.core.api.DTO.req;

import lombok.Data;

@Data
public class CreateOrgRequest {


    private String orgName;

    private String remark;

    private String managerName;

    private String email;

    private String phone;
}
