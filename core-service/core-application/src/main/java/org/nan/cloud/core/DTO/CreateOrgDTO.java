package org.nan.cloud.core.DTO;

import lombok.Data;
import org.nan.cloud.core.domain.Organization;

@Data
public class CreateOrgDTO {

    private String orgName;

    private String remark;

    private String managerName;

    private String managerPsw;

    private String email;

    private String phone;

    private Long ugid;

    private Long tgid;

    private Integer suffix;

    public void fillOrgInfo(Organization organization) {
        setUgid(organization.getRootUgid());
        setTgid(organization.getRootTgid());
        setSuffix(organization.getSuffix());
    }

}
