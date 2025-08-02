package org.nan.cloud.core.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateFolderDTO {

    private Long oid;

    private Long uid;

    private Long targetUgid;

    private Long targetFid;

    private String folderName;

    private String description;
}
