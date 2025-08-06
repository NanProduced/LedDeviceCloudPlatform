package org.nan.cloud.core.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.nan.cloud.common.basic.model.PageRequestDTO;
import org.nan.cloud.common.basic.model.PageVO;
import org.nan.cloud.core.api.DTO.req.QueryBroadcastMessageRequest;
import org.nan.cloud.core.api.DTO.res.BroadcastMessageResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.Map;

/**
 * 广播消息API接口
 * 
 * @author LedDeviceCloudPlatform Team  
 * @since 1.0.0
 */
@Tag(name = "广播消息API", description = "提供广播消息查询、统计等功能")
public interface BroadcastMessageApi {

    String prefix = "/broadcast/message";

    @Operation(summary = "获取用户广播消息列表")
    @PostMapping(prefix + "/list")
    PageVO<BroadcastMessageResponse> getUserBroadcastMessages(PageRequestDTO<QueryBroadcastMessageRequest> requestDTO);

    @Operation(summary = "获取用户未读广播消息数量")
    @GetMapping(prefix + "/unread/count")
    Long getUnreadCount();

    @Operation(summary = "按类型获取用户未读广播消息数量")
    @GetMapping(prefix + "/unread/count-by-type")
    Map<String, Long> getUnreadCountByType();
}