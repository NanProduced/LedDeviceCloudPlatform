package org.nan.cloud.core.api.quota.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuotaCheckResponse {
    private boolean allowed;

    private Long remainingBytes;
    private Integer remainingFiles;

    private Long usedBytes;
    private Integer usedFiles;

    private Long maxBytes;
    private Integer maxFiles;

    private String message;
}

