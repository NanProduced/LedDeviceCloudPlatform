package org.nan.cloud.core.DTO;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class QueryTerminalListDTO {

    private Long oid;

    private Long tgid;

    private boolean ifIncludeSubGroups;

    private String keyword;

    private String terminalModel;

    private Integer onlineStatus;
}