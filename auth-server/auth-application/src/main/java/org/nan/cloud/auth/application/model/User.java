package org.nan.cloud.auth.application.model;

import lombok.Data;

@Data
public class User {

    private Long uid;

    private Long oid;

    private String username;

    private String password;

    private Long ugid;

    private Long suffix;

    private String email;

    private String phone;

    private Integer status;

}
