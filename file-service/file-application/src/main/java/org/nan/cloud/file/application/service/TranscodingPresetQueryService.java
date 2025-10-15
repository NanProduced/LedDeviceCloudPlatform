package org.nan.cloud.file.application.service;

import org.nan.cloud.file.api.dto.TranscodingPresetResponse;

public interface TranscodingPresetQueryService {
    TranscodingPresetResponse listPresets();

    TranscodingPresetResponse.PresetInfo getPresetById(String id);

    /**
     * 返回用于前端构造请求的“参数模板”，将预设展开为可编辑字段集合
     */
    org.nan.cloud.file.api.dto.TranscodingTaskRequest.TranscodingParameters toRequestParameters(String presetId);
}

