package org.nan.cloud.core.api.quota.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuotaCheckRequest {
    private Long orgId;
    private Long bytes;
    private Integer files;
}

