package org.nan.cloud.core.infrastructure.repository.mongo.document;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Map;

@Data
@Builder
@Document("operation_permission")
public class OperationPermissionDocument {

    @Id
    private String objectId;

    private Long operationPermissionId;

    private String name;

    private String description;

    private String operationType;

    private Map<Long, String> permissions;
}
