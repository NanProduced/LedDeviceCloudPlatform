package org.nan.cloud.core.infrastructure.repository.mongo.repository;

import lombok.RequiredArgsConstructor;
import org.nan.cloud.core.infrastructure.repository.mongo.document.OperationPermissionDocument;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class OperationPermissionMongoRepository {

    private final MongoTemplate mongoTemplate;

    public Map<Long, String> getPermissionsByOperationPermissionId(Long operationPermissionId) {
        Query query = Query.query(Criteria.where("operationPermissionId").is(operationPermissionId));
        OperationPermissionDocument one = mongoTemplate.findOne(query, OperationPermissionDocument.class);
        return one == null ? null : one.getPermissions();
    }

    public Map<Long, OperationPermissionDocument> getOperationPermissionDocumentsByOpIds(List<Long> opIds) {
        Query query = Query.query(Criteria.where("operationPermissionId").in(opIds));
        return mongoTemplate.find(query, OperationPermissionDocument.class)
                .stream()
                .collect(Collectors.toMap(OperationPermissionDocument::getOperationPermissionId, Function.identity()));
    }

    public Set<Long> getPermissionIdsByOpIds(List<Long> opIds) {
        Query query = Query.query(Criteria.where("operationPermissionId").in(opIds));
        return mongoTemplate.find(query, OperationPermissionDocument.class)
                .stream()
                .flatMap(doc -> doc.getPermissions().keySet().stream())
                .collect(Collectors.toSet());
    }
}
