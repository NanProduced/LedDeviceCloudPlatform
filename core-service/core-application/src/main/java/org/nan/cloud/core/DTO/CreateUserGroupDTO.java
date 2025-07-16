package org.nan.cloud.core.DTO;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CreateUserGroupDTO {

    private Long oid;

    private Long parentUgid;

    private String ugName;

    private String description;

    private Long creatorUid;
}
