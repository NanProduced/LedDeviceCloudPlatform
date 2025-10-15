package org.nan.cloud.core.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.nan.cloud.common.basic.model.PageRequestDTO;
import org.nan.cloud.common.basic.model.PageVO;
import org.nan.cloud.core.api.DTO.req.QueryRealtimeMessageRequest;
import org.nan.cloud.core.api.DTO.res.RealtimeMessageResponse;
import org.nan.cloud.core.api.RealtimeMessageApi;
import org.nan.cloud.core.facade.RealtimeFacade;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Tag(name = "Realtime(实时消息控制器)", description = "实时消息相关操作")
@RestController
@RequiredArgsConstructor
public class RealtimeMessageController implements RealtimeMessageApi {

    private final RealtimeFacade realtimeFacade;

    @Operation(
            summary = "查询用户实时消息列表",
            description = "查询用户的实时消息列表",
            tags = {"实时消息", "通用接口"}
    )
    @Override
    public PageVO<RealtimeMessageResponse> getUserMessages(PageRequestDTO<QueryRealtimeMessageRequest> requestDTO) {
        return realtimeFacade.getUserMessages(requestDTO);
    }

    @Operation(
            summary = "查询用户未读消息（前端页面右上角消息小红点）",
            description = "查询用户的未读消息数量",
            tags = {"实时消息", "通用接口"}
    )
    @Override
    public Long getUnreadCount() {
        return realtimeFacade.getUnreadCount();
    }

    @Operation(
            summary = "查询用户未读消息统计(消息中心-实时消息页面统计数据卡片)",
            description = "查询用户的未读消息 - 按类型分类统计",
            tags = {"实时消息", "通用接口"}
    )
    @Override
    public Map<String, Long> getUnreadCountMap() {
        return realtimeFacade.getUnreadCountMap();
    }
}
