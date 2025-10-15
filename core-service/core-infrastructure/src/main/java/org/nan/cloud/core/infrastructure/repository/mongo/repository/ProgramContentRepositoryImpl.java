package org.nan.cloud.core.infrastructure.repository.mongo.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nan.cloud.core.repository.ProgramContentRepository;
import org.nan.cloud.program.document.ProgramContent;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 节目内容Repository实现
 * 符合DDD Infrastructure层职责：处理MongoDB数据持久化
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class ProgramContentRepositoryImpl implements ProgramContentRepository {

    private final MongoTemplate mongoTemplate;

    @Override
    public List<ProgramContent> findByProgramId(Long programId) {
        log.debug("Finding content by program id: {}", programId);
        
        Query query = new Query(Criteria.where("program_id").is(programId));
        query.with(org.springframework.data.domain.Sort.by(
                org.springframework.data.domain.Sort.Direction.DESC, "version"));
        
        return mongoTemplate.find(query, ProgramContent.class);
    }

    @Override
    public Optional<ProgramContent> findByProgramIdAndVersion(Long programId, Integer version) {
        log.debug("Finding content by program id: {} and version: {}", programId, version);
        
        Query query = new Query(Criteria.where("program_id").is(programId)
                                       .and("version").is(version));
        
        ProgramContent content = mongoTemplate.findOne(query, ProgramContent.class);
        return Optional.ofNullable(content);
    }

    @Override
    public Optional<ProgramContent> findLatestVersionByProgramId(Long programId) {
        log.debug("Finding latest version content by program id: {}", programId);
        
        Query query = new Query(Criteria.where("program_id").is(programId));
        query.with(org.springframework.data.domain.Sort.by(
                org.springframework.data.domain.Sort.Direction.DESC, "version"));
        query.limit(1);
        
        ProgramContent content = mongoTemplate.findOne(query, ProgramContent.class);
        return Optional.ofNullable(content);
    }

    @Override
    public Optional<ProgramContent> findById(String id) {
        log.debug("Finding content by id: {}", id);
        
        ProgramContent content = mongoTemplate.findById(id, ProgramContent.class);
        return Optional.ofNullable(content);
    }

    @Override
    public ProgramContent save(ProgramContent content) {
        log.debug("Saving program content: programId={}, version={}", 
                content.getProgramId(), content.getVersion());
        
        if (!StringUtils.hasText(content.getId())) {
            // 新文档，设置创建时间
            content.setCreatedTime(LocalDateTime.now());
        }
        content.setUpdatedTime(LocalDateTime.now());
        
        ProgramContent savedContent = mongoTemplate.save(content);
        log.debug("Program content saved: id={}", savedContent.getId());
        
        return savedContent;
    }

    @Override
    public int updateVsnXml(Long programId, Integer version, String vsnXml) {
        log.debug("Updating VSN XML: programId={}, version={}", programId, version);
        
        Query query = new Query(Criteria.where("program_id").is(programId)
                                       .and("version").is(version));
        
        Update update = new Update()
                .set("vsn_xml", vsnXml)
                .set("updated_time", LocalDateTime.now());
        
        var result = mongoTemplate.updateFirst(query, update, ProgramContent.class);
        
        int modifiedCount = (int) result.getModifiedCount();
        log.debug("VSN XML updated: programId={}, version={}, modified={}", 
                programId, version, modifiedCount);
        
        return modifiedCount;
    }

    @Override
    public int updateVsnXmlBatch(Map<String, String> updates) {
        log.debug("Batch updating VSN XML: count={}", updates.size());
        
        int totalUpdated = 0;
        
        for (Map.Entry<String, String> entry : updates.entrySet()) {
            String documentId = entry.getKey();
            String vsnXml = entry.getValue();
            
            Query query = new Query(Criteria.where("id").is(documentId));
            Update update = new Update()
                    .set("vsn_xml", vsnXml)
                    .set("updated_time", LocalDateTime.now());
            
            var result = mongoTemplate.updateFirst(query, update, ProgramContent.class);
            totalUpdated += result.getModifiedCount();
        }
        
        log.debug("Batch VSN XML update completed: requested={}, updated={}", 
                updates.size(), totalUpdated);
        
        return totalUpdated;
    }

    @Override
    public int deleteById(String id) {
        log.debug("Deleting content by id: {}", id);
        
        Query query = new Query(Criteria.where("id").is(id));
        var result = mongoTemplate.remove(query, ProgramContent.class);
        
        int deletedCount = (int) result.getDeletedCount();
        log.debug("Content deleted: id={}, deleted={}", id, deletedCount);
        
        return deletedCount;
    }

    @Override
    public int deleteByProgramId(Long programId) {
        log.debug("Deleting all content by program id: {}", programId);
        
        Query query = new Query(Criteria.where("program_id").is(programId));
        var result = mongoTemplate.remove(query, ProgramContent.class);
        
        int deletedCount = (int) result.getDeletedCount();
        log.debug("All content deleted for program: id={}, deleted={}", programId, deletedCount);
        
        return deletedCount;
    }

    @Override
    public int deleteByProgramIdAndVersion(Long programId, Integer version) {
        log.debug("Deleting content by program id: {} and version: {}", programId, version);
        
        Query query = new Query(Criteria.where("program_id").is(programId)
                                       .and("version").is(version));
        var result = mongoTemplate.remove(query, ProgramContent.class);
        
        int deletedCount = (int) result.getDeletedCount();
        log.debug("Content deleted: programId={}, version={}, deleted={}", 
                programId, version, deletedCount);
        
        return deletedCount;
    }

    @Override
    public List<Integer> findVersionsByProgramId(Long programId) {
        log.debug("Finding versions by program id: {}", programId);
        
        Query query = new Query(Criteria.where("program_id").is(programId));
        query.fields().include("version");
        query.with(org.springframework.data.domain.Sort.by(
                org.springframework.data.domain.Sort.Direction.ASC, "version"));
        
        List<ProgramContent> contents = mongoTemplate.find(query, ProgramContent.class);
        return contents.stream()
                      .map(ProgramContent::getVersion)
                      .toList();
    }

    @Override
    public Optional<Integer> findMaxVersionByProgramId(Long programId) {
        log.debug("Finding max version by program id: {}", programId);
        
        Query query = new Query(Criteria.where("program_id").is(programId));
        query.fields().include("version");
        query.with(org.springframework.data.domain.Sort.by(
                org.springframework.data.domain.Sort.Direction.DESC, "version"));
        query.limit(1);
        
        ProgramContent content = mongoTemplate.findOne(query, ProgramContent.class);
        return Optional.ofNullable(content)
                      .map(ProgramContent::getVersion);
    }

    @Override
    public boolean existsByProgramIdAndVersion(Long programId, Integer version) {
        log.debug("Checking existence: programId={}, version={}", programId, version);
        
        Query query = new Query(Criteria.where("program_id").is(programId)
                                       .and("version").is(version));
        
        boolean exists = mongoTemplate.exists(query, ProgramContent.class);
        log.debug("Content exists: programId={}, version={}, exists={}", 
                programId, version, exists);
        
        return exists;
    }

    @Override
    public long countVersionsByProgramId(Long programId) {
        log.debug("Counting versions by program id: {}", programId);
        
        Query query = new Query(Criteria.where("program_id").is(programId));
        
        long count = mongoTemplate.count(query, ProgramContent.class);
        log.debug("Version count: programId={}, count={}", programId, count);
        
        return count;
    }
}