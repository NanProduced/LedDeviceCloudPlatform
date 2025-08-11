package org.nan.cloud.file.application.repository;

import org.nan.cloud.common.basic.domain.TranscodingDetail;

/**
 * 转码详情仓储接口（MongoDB）
 */
public interface TranscodingDetailRepository {

    String COLLECTION_NAME = "material_transcode_detail";

    String save(TranscodingDetail detail);

    TranscodingDetail findById(String id);

    TranscodingDetail findByTaskId(String taskId);

    boolean update(TranscodingDetail detail);
}

