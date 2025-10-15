package org.nan.cloud.terminal.application.domain;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TerminalInfo {

    private Long tid;

    private String terminalName;

    private String description;

    private String terminalModel;

    private Long oid;

    private Long tgid;

    private String firmwareVersion;

    private String serialNumber;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private Long createdBy;

    private Long updatedBy;

    private Integer deleted;
}
