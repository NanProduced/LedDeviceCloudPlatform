package org.nan.cloud.file.infrastructure.repository.mongodb;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nan.cloud.common.basic.domain.TranscodingDetail;
import org.nan.cloud.file.application.repository.TranscodingDetailRepository;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

@Slf4j
@Repository
@RequiredArgsConstructor
public class TranscodingDetailRepositoryImpl implements TranscodingDetailRepository {

    private final MongoTemplate mongoTemplate;

    @Override
    public String save(TranscodingDetail detail) {
        TranscodingDetail saved = mongoTemplate.save(detail, COLLECTION_NAME);
        return saved.getId();
    }

    @Override
    public TranscodingDetail findById(String id) {
        return mongoTemplate.findById(id, TranscodingDetail.class, COLLECTION_NAME);
    }

    @Override
    public TranscodingDetail findByTaskId(String taskId) {
        Query query = Query.query(Criteria.where("taskId").is(taskId));
        return mongoTemplate.findOne(query, TranscodingDetail.class, COLLECTION_NAME);
    }

    @Override
    public boolean update(TranscodingDetail detail) {
        if (detail.getId() == null) {
            return false;
        }
        Query query = Query.query(Criteria.where("_id").is(detail.getId()));
        Update update = new Update()
                .set("taskId", detail.getTaskId())
                .set("oid", detail.getOid())
                .set("uid", detail.getUid())
                .set("sourceMaterialId", detail.getSourceMaterialId())
                .set("sourceFileId", detail.getSourceFileId())
                .set("targetFileId", detail.getTargetFileId())
                .set("presetName", detail.getPresetName())
                .set("parameters", detail.getParameters())
                .set("engine", detail.getEngine())
                .set("metrics", detail.getMetrics())
                .set("status", detail.getStatus())
                .set("errorMessage", detail.getErrorMessage())
                .set("createdAt", detail.getCreatedAt())
                .set("completedAt", detail.getCompletedAt())
                .set("thumbnailPath", detail.getThumbnailPath());
        return mongoTemplate.updateFirst(query, update, TranscodingDetail.class, COLLECTION_NAME)
                .getModifiedCount() > 0;
    }
}

