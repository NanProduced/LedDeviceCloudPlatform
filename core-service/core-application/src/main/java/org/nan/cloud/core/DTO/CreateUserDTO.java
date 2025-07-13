package org.nan.cloud.core.DTO;

import java.util.List;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CreateUserDTO {

    private Long oid;

    private Integer suffix;

    private Long ugid;

    private String username;

    private String encodePassword;

    private String email;

    private String phone;

    private Long creatorId;
    
}
