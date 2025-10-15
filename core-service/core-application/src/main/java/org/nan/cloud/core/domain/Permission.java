package org.nan.cloud.core.domain;

import lombok.Data;

@Data
public class Permission {

    private Long permissionId;

    private String name;

    private String url;

    private String method;

    private String description;

    private String permissionGroup;

    private Integer permissionType;

}
