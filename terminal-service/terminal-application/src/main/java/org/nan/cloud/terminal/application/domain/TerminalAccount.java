package org.nan.cloud.terminal.application.domain;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TerminalAccount {

    private Long tid;

    private String account;

    private String password;

    private Integer status;

    private Long oid;

    private LocalDateTime firstLoginTime;

    private LocalDateTime lastLoginTime;

    private String lastLoginIp;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private Long createBy;

    private Long updateBy;

    private Boolean deleted;
}
