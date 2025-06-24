package org.nan.cloud.core.DTO;

import lombok.Data;

@Data
public class CreateOrgDTO {

    private String orgName;

    private String remark;

    private String managerName;

    private String managerPsw;

}
