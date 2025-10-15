package org.nan.cloud.common.web.context;

import lombok.Data;

@Data
public class RequestUserInfo {

    private Long uid;

    private Long ugid;

    private Long oid;

    private Integer userType;
}
