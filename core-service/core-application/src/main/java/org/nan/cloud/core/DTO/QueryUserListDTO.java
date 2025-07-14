package org.nan.cloud.core.DTO;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class QueryUserListDTO {

    private Long oid;

    private Long ugid;

    private boolean ifIncludeSubGroups;

    private String userNameKeyword;

    private String emailKeyword;
}
