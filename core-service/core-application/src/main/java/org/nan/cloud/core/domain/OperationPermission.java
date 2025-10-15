package org.nan.cloud.core.domain;

import lombok.Data;

import java.util.Map;

@Data
public class OperationPermission {

    private Long operationPermissionId;

    private String name;

    private String description;

    private String operationType;

    private String createTime;

    private Map<Long, String> permissions;
}
