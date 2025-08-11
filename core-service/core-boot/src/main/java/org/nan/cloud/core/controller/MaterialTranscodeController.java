package org.nan.cloud.core.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.nan.cloud.common.web.context.InvocationContextHolder;
import org.nan.cloud.common.web.context.RequestUserInfo;
import org.nan.cloud.core.domain.Task;
import org.nan.cloud.core.enums.TaskStatusEnum;
import org.nan.cloud.core.enums.TaskTypeEnum;
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

@Tag(name = "Material Transcoding")
@RestController
@RequiredArgsConstructor
@RequestMapping("/material")
public class MaterialTranscodeController {

    private final TaskService taskService;
    private final MessageProducer messageProducer;

    private static final String BUSINESS_EXCHANGE = "business.topic";

    @Operation(summary = "提交素材转码任务")
    @PostMapping("/{mid}/transcode")
    public TaskInitResponse submitTranscode(@PathVariable("mid") Long materialId) {
        RequestUserInfo user = InvocationContextHolder.getContext().getRequestUser();

        String taskId = UUID.randomUUID().toString();

        Task task = Task.builder()
                .taskId(taskId)
                .taskType(TaskTypeEnum.MATERIAL_TRANSCODE)
                .taskStatus(TaskStatusEnum.PENDING)
                .oid(user.getOid())
                .ref("material:" + materialId)
                .refId(String.valueOf(materialId))
                .creator(user.getUid())
                .progress(0)
                .createTime(LocalDateTime.now())
                .build();
        taskService.createTask(task);

        Map<String, Object> payload = new HashMap<>();
        payload.put("eventType", "CREATED");
        payload.put("taskId", taskId);
        payload.put("organizationId", String.valueOf(user.getOid()));
        payload.put("userId", String.valueOf(user.getUid()));
        payload.put("sourceMaterialId", materialId);

        Message message = Message.builder()
                .messageType("FILE_TRANSCODING_CREATED")
                .payload(payload)
                .organizationId(String.valueOf(user.getOid()))
                .exchange(BUSINESS_EXCHANGE)
                .routingKey(String.format("file.transcoding.created.%s.%s", user.getOid(), materialId))
                .priority(3)
                .sourceSystem("core-service")
                .targetSystem("file-service")
                .build();
        messageProducer.send(message);

        return TaskInitResponse.builder()
                .taskId(taskId)
                .taskType("MATERIAL_TRANSCODE")
                .status("PENDING")
                .progressSubscriptionUrl("/topic/task/" + taskId)
                .message("任务已创建，等待 file-service 执行")
                .build();
    }
}

