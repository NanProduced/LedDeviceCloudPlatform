package org.nan.cloud.core.api.DTO.req;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuotaCheckRequest {
    private Long bytes;
    private Integer files;
}

