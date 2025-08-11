package org.nan.cloud.file.api;

import org.nan.cloud.file.api.dto.TranscodingPresetResponse;
import org.nan.cloud.file.api.dto.TranscodingTaskRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

public interface TranscodeApi {

    String prefix = "/file/transcode";

    @GetMapping(prefix + "/presets")
    TranscodingPresetResponse getPresets();

    @GetMapping(prefix + "/presets/{id}")
    TranscodingPresetResponse.PresetInfo getPresetById(@PathVariable("id") String id);

    @GetMapping(prefix + "/presets/{id}/parameters")
    TranscodingTaskRequest.TranscodingParameters getPresetParameters(@PathVariable("id") String id);
}
