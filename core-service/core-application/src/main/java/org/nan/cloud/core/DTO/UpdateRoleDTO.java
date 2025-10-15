package org.nan.cloud.core.DTO;

import lombok.Builder;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

@Data
@Builder
public class UpdateRoleDTO {

    private Long rid;

    private String roleName;

    private String description;

    private Long updaterUid;

    public boolean needToUpdateRole() {
        return StringUtils.isNotBlank(roleName) || StringUtils.isNotBlank(description);
    }
}
