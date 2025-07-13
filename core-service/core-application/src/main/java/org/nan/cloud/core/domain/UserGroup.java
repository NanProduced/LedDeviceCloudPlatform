package org.nan.cloud.core.domain;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Data
@Builder
public class UserGroup {

    private Long ugid;

    private String name;

    private Long oid;

    private Long parent;

    private String path;

    private String description;

    /**
     * 0：org root
     * 1: normal
     */
    private Integer type;

    private Long creatorId;

    private LocalDateTime createTime;

    public List<Long> parsePathToIdList() {
        return Arrays.stream(path.split("\\|"))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .map(Long::valueOf)
                .collect(Collectors.toList());
    }
}
