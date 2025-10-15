package org.nan.cloud.core.DTO;

import lombok.Builder;
import lombok.Data;

import java.util.Collection;
import java.util.Map;

@Data
@Builder
public class UserGroupRelDTO {

    private Long ugid;

    private String ugName;

    private Long parent;

    private String path;

    /**
     * key: ugid
     * value: ugName
     */
    private Map<Long, String> pathMap;
}
