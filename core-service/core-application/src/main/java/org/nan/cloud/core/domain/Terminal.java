package org.nan.cloud.core.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Terminal {

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

    private Integer onlineStatus;
}
