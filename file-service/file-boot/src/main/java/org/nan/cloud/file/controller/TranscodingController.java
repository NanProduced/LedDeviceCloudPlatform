package org.nan.cloud.file.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.nan.cloud.file.api.TranscodeApi;
import org.nan.cloud.file.api.dto.TranscodingPresetResponse;
import org.nan.cloud.file.api.dto.TranscodingTaskRequest;
import org.nan.cloud.file.application.service.TranscodingPresetQueryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;


@Tag(name = "Transcoding(转码)")
@RestController
@RequiredArgsConstructor
public class TranscodingController implements TranscodeApi {

    private static final String PREFIX = "/file/transcoding";

    private final TranscodingPresetQueryService presetQueryService;

    @Operation(
            summary = "获取转码预设列表",
            description = "给前端返回转码预设列表",
            tags = {"转码操作"}
    )
    public TranscodingPresetResponse getPresets() {
        return presetQueryService.listPresets();
    }

    @Operation(
            summary = "按ID获取转码预设",
            tags = {"转码操作"}
    )
    public TranscodingPresetResponse.PresetInfo getPresetById(@PathVariable("id") String id) {
        return presetQueryService.getPresetById(id);
    }

    @Operation(
            summary = "将预设展开为可编辑参数模板",
            tags = {"转码操作"}
    )
    public TranscodingTaskRequest.TranscodingParameters getPresetParameters(@PathVariable("id") String id) {
        return presetQueryService.toRequestParameters(id);
    }
}

