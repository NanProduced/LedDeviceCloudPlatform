package org.nan.cloud.file.application.repository;

import org.nan.cloud.program.document.ProgramContent;

/**
 * 节目内容仓储接口（Mongo）
 * 由 infrastructure 层使用 MongoTemplate 实现
 */
public interface ProgramContentRepository {
    ProgramContent findById(String contentId);
}

