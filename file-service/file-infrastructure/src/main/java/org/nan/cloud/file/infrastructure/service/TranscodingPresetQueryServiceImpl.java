package org.nan.cloud.file.infrastructure.service;

import lombok.RequiredArgsConstructor;
import org.nan.cloud.file.api.dto.TranscodingPresetResponse;
import org.nan.cloud.file.application.config.FileStorageProperties;
import org.nan.cloud.file.application.service.TranscodingPresetQueryService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TranscodingPresetQueryServiceImpl implements TranscodingPresetQueryService {

    private final FileStorageProperties properties;

    @Override
    public TranscodingPresetResponse listPresets() {
        List<FileStorageProperties.Transcoding.Preset> presets = properties.getTranscoding().getPresets();
        List<TranscodingPresetResponse.PresetInfo> list = presets == null ? List.of() : presets.stream()
                .map(this::toPresetInfo)
                .collect(Collectors.toList());
        return TranscodingPresetResponse.builder()
                .presets(list)
                .supportedFormats(List.of("mp4", "mov", "mkv"))
                .build();
    }

    @Override
    public TranscodingPresetResponse.PresetInfo getPresetById(String id) {
        List<FileStorageProperties.Transcoding.Preset> presets = properties.getTranscoding().getPresets();
        if (presets == null) {
            return null;
        }
        return presets.stream()
                .filter(p -> Objects.equals(p.getId(), id))
                .findFirst()
                .map(this::toPresetInfo)
                .orElse(null);
    }

    private TranscodingPresetResponse.PresetInfo toPresetInfo(FileStorageProperties.Transcoding.Preset p) {
        return TranscodingPresetResponse.PresetInfo.builder()
                .name(p.getId())
                .displayName(p.getName())
                .description(null)
                .videoConfig(TranscodingPresetResponse.VideoConfig.builder()
                        .resolution(p.getWidth() + "x" + p.getHeight())
                        .width(p.getWidth())
                        .height(p.getHeight())
                        .bitrate(p.getVideoBitrate())
                        .frameRate(null)
                        .codec(p.getVideoCodec())
                        .quality(p.getPreset())
                        .crf(p.getCrf())
                        .speedPreset(p.getPreset())
                        .container("mp4")
                        .build())
                .audioConfig(TranscodingPresetResponse.AudioConfig.builder()
                        .bitrate(p.getAudioBitrate())
                        .sampleRate(null)
                        .channels(null)
                        .codec(p.getAudioCodec())
                        .build())
                .isDefault(false)
                .scene("LED")
                .estimatedSizeRatio(null)
                .build();
    }

    @Override
    public org.nan.cloud.file.api.dto.TranscodingTaskRequest.TranscodingParameters toRequestParameters(String presetId) {
        List<FileStorageProperties.Transcoding.Preset> presets = properties.getTranscoding().getPresets();
        if (presets == null) return null;
        var opt = presets.stream().filter(p -> java.util.Objects.equals(p.getId(), presetId)).findFirst();
        if (opt.isEmpty()) return null;
        var p = opt.get();
        var params = new org.nan.cloud.file.api.dto.TranscodingTaskRequest.TranscodingParameters();
        params.setVideoCodec(p.getVideoCodec());
        params.setAudioCodec(p.getAudioCodec());
        params.setVideoBitrate(p.getVideoBitrate());
        params.setAudioBitrate(p.getAudioBitrate());
        params.setWidth(p.getWidth());
        params.setHeight(p.getHeight());
        params.setPreset(p.getPreset());
        params.setCrf(p.getCrf());
        return params;
    }
}

