package org.nan.cloud.terminal.infrastructure.persistence.mongodb.repository;

import lombok.RequiredArgsConstructor;
import org.nan.cloud.common.basic.utils.BeanUtils;
import org.nan.cloud.terminal.application.domain.TerminalStatusReport;
import org.nan.cloud.terminal.application.repository.TerminalReportRepository;
import org.nan.cloud.terminal.cache.TerminalStatusCacheHandler;
import org.nan.cloud.terminal.infrastructure.persistence.mongodb.document.TerminalStatusReportDocument;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Objects;

@Repository
@RequiredArgsConstructor
public class TerminalReportRepositoryImpl implements TerminalReportRepository {

    private final MongoTemplate mongoTemplate;

    private final TerminalStatusCacheHandler terminalStatusCacheHandler;

    @Override
    public TerminalStatusReport getTerminalStatusReportByTid(Long oid, Long tid) {
        Query query = new Query(Criteria.where("oid").is(oid)
                .and("tid").is(tid));
        TerminalStatusReportDocument doc = mongoTemplate.findOne(query, TerminalStatusReportDocument.class);
        if (Objects.nonNull(doc) && Objects.nonNull(doc.getTerminalStatusReport())) {
            return doc.getTerminalStatusReport();
        }
        else return null;
    }

    /**
     * 更新终端状态上报
     * @param oid
     * @param tid
     * @param terminalName
     * @param terminalStatusReport
     */
    @Override
    public void updateTerminalStatusReport(Long oid, Long tid, String terminalName, TerminalStatusReport terminalStatusReport) {
        // 尝试更新redis缓存，获取更新结果
        boolean cacheResult = terminalStatusCacheHandler.tryUpdateTerminalStatusReport(oid, tid, terminalStatusReport);
        // 更新失败
        if (!cacheResult) {
            // mongoDB存在数据说明是上线终端上报状态，更新状态并缓存
            TerminalStatusReportDocument doc = mongoTemplate.findOne(Query.query(Criteria.where("oid").is(oid).and("tid").is(tid)), TerminalStatusReportDocument.class);
            if (Objects.nonNull(doc)) {
                BeanUtils.copyNonNullProperties(terminalStatusReport, doc.getTerminalStatusReport());
                terminalStatusCacheHandler.cacheTerminalStatusReport(oid, tid, doc.getTerminalStatusReport());
            }
            // mongoDB不存在数据说明是终端初始化上报状态，缓存新状态
            else {
                terminalStatusCacheHandler.cacheTerminalStatusReport(oid, tid, terminalStatusReport);
            }
        }
        // 异步MongoDB保存上报状态数据
        asyncUpsertTerminalStatusReport(oid, tid, terminalName, terminalStatusReport);

    }

    @Async("MongoAsyncThreadPool")
    public void asyncUpsertTerminalStatusReport(Long oid, Long tid, String terminalName, TerminalStatusReport terminalStatusReport) {
        Query query = Query.query(Criteria.where("oid").is(oid).and("tid").is(tid));
        TerminalStatusReportDocument doc = mongoTemplate.findOne(query, TerminalStatusReportDocument.class);
        if (Objects.nonNull(doc)) {
            BeanUtils.copyNonNullProperties(terminalStatusReport, doc.getTerminalStatusReport());
            doc.setUpdateTime(LocalDateTime.now());
            mongoTemplate.save(doc);
        }
        else {
            TerminalStatusReportDocument newDoc = new TerminalStatusReportDocument();
            newDoc.setOid(oid);
            newDoc.setTid(tid);
            newDoc.setTerminalName(terminalName);
            newDoc.setTerminalStatusReport(terminalStatusReport);
            newDoc.setUpdateTime(LocalDateTime.now());
            mongoTemplate.save(newDoc);
        }
    }
}
