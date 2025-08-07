package org.nan.cloud.core.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nan.cloud.common.basic.model.PageRequestDTO;
import org.nan.cloud.common.basic.model.PageVO;
import org.nan.cloud.core.api.BroadcastMessageApi;
import org.nan.cloud.core.api.DTO.req.QueryBroadcastMessageRequest;
import org.nan.cloud.core.api.DTO.res.BroadcastMessageResponse;
import org.nan.cloud.core.facade.BroadcastFacade;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 广播消息控制器
 * 
 * 提供广播消息相关的REST API接口
 * 
 * @author LedDeviceCloudPlatform Team
 * @since 1.0.0
 */
@Tag(name = "广播消息管理", description = "广播消息查询、统计等功能")
@Slf4j
@RestController
@RequiredArgsConstructor
public class BroadcastMessageController implements BroadcastMessageApi {

    private final BroadcastFacade broadcastFacade;

    @Override
    @Operation(summary = "获取用户广播消息列表",
            description = "分页查询当前用户的广播消息，支持类型筛选和已读状态筛选",
            tags = {"通知消息", "通用接口"}
    )
    @PostMapping(prefix + "/list")
    public PageVO<BroadcastMessageResponse> getUserBroadcastMessages(
            @RequestBody PageRequestDTO<QueryBroadcastMessageRequest> requestDTO) {
        
        log.debug("收到获取用户广播消息列表请求 - 页码: {}, 大小: {}",
                requestDTO.getPageNum(), requestDTO.getPageSize());
        
        return broadcastFacade.getUserBroadcastMessages(requestDTO);
    }

    @Override
    @Operation(
            summary = "获取用户未读广播消息数量",
            description = "获取当前用户未读的广播消息总数",
            tags = {"通知消息", "通用接口"}
    )
    @GetMapping(prefix + "/unread/count")
    public Long getUnreadCount() {
        log.debug("收到获取用户未读广播消息数量请求");
        
        return broadcastFacade.getUnreadCount();
    }

    @Override
    @Operation(summary = "按类型获取用户未读广播消息数量",
            description = "获取当前用户按消息类型分组的未读消息数量",
            tags = {"通知消息", "通用接口"}
    )
    @GetMapping(prefix + "/unread/count-by-type")
    public Map<String, Long> getUnreadCountByType() {
        log.debug("收到按类型获取用户未读广播消息数量请求");
        
        return broadcastFacade.getUnreadCountByType();
    }

    @Operation(summary = "获取广播消息详情",
            description = "根据消息ID获取广播消息的详细信息",
            tags = {"通知消息", "通用接口"}
    )
    @GetMapping(prefix + "/detail/{messageId}")
    public BroadcastMessageResponse getMessageDetail(@PathVariable String messageId) {
        log.debug("收到获取广播消息详情请求 - 消息ID: {}", messageId);
        
        return broadcastFacade.getMessageDetail(messageId);
    }
}