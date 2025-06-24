package org.nan.cloud.core.domain;

import lombok.Data;

@Data
public class Role {

    private Long rid;

    private Long oid;

    private String name;

    /**
     * 0: system
     * 1: customer
     */
    private Integer type;
}
