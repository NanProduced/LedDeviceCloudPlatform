package org.nan.cloud.core.api;

import org.nan.cloud.common.basic.model.PageRequestDTO;
import org.nan.cloud.common.basic.model.PageVO;
import org.nan.cloud.core.api.DTO.req.QueryRealtimeMessageRequest;
import org.nan.cloud.core.api.DTO.res.RealtimeMessageResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.Map;

public interface RealtimeMessageApi {

    String prefix = "/realtime/message";

    @PostMapping(prefix + "/list")
    PageVO<RealtimeMessageResponse> getUserMessages(PageRequestDTO<QueryRealtimeMessageRequest> requestDTO);

    @GetMapping(prefix + "/unread")
    Long getUnreadCount();

    @GetMapping(prefix + "/count")
    Map<String, Long> getUnreadCountMap();
}
