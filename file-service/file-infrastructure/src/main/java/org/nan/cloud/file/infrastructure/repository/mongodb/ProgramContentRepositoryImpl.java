package org.nan.cloud.file.infrastructure.repository.mongodb;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nan.cloud.file.application.repository.ProgramContentRepository;
import org.nan.cloud.program.document.ProgramContent;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

@Slf4j
@Repository
@RequiredArgsConstructor
public class ProgramContentRepositoryImpl implements ProgramContentRepository {

    private final MongoTemplate mongoTemplate;

    @Override
    public ProgramContent findById(String contentId) {
        return mongoTemplate.findById(contentId, ProgramContent.class, "program_contents");
    }

    @Override
    public ProgramContent findByProgramId(Long programId) {
        return mongoTemplate.findOne(Query.query(Criteria.where("program_id").is(programId)), ProgramContent.class);
    }
}

