package org.nan.cloud.core.api.DTO.res;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.nan.cloud.core.api.DTO.common.RoleDTO;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "用户列表查询结果")
@Data
public class UserListResponse {

    private Long uid;

    private String username;

    private Long ugid;

    private String ugName;

    private String phone;

    private String email;

    private Integer status;

    private Integer type;

    private List<RoleDTO> roles;

    private LocalDateTime updateTime;

    private LocalDateTime createTime;
}
