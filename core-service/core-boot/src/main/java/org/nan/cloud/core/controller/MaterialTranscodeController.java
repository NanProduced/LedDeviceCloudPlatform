package org.nan.cloud.core.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.nan.cloud.common.web.context.InvocationContextHolder;
import org.nan.cloud.common.web.context.RequestUserInfo;
import org.nan.cloud.core.api.MaterialTranscodeApi;
import org.nan.cloud.core.domain.Task;
import org.nan.cloud.core.enums.TaskStatusEnum;
import org.nan.cloud.core.enums.TaskTypeEnum;
import org.nan.cloud.core.facade.MaterialFacade;
import org.nan.cloud.core.service.TaskService;
import org.nan.cloud.common.mq.producer.MessageProducer;
import org.nan.cloud.common.mq.core.message.Message;
import org.nan.cloud.core.api.DTO.res.TaskInitResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Tag(name = "material transcoding(素材转码)")
@RestController
@RequiredArgsConstructor
public class MaterialTranscodeController implements MaterialTranscodeApi {

    private final MaterialFacade materialFacade;

    @Operation(
            summary = "提交素材转码任务",
            description = "选择素材进行转码",
            tags = {"素材管理", "素材转码"}
    )
    public TaskInitResponse submitTranscode(@PathVariable("mid") Long materialId) {
        return materialFacade.submitTranscode(materialId);
    }
}

