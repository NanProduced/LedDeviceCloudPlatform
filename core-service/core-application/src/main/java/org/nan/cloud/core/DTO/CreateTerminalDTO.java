package org.nan.cloud.core.DTO;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CreateTerminalDTO {

    private String terminalName;

    private String description;

    private String terminalAccount;

    private String terminalPassword;

    private Long tgid;

    private Long oid;

    private Long createdBy;
}
