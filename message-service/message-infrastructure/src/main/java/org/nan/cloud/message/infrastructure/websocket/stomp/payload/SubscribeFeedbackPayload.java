package org.nan.cloud.message.infrastructure.websocket.stomp.payload;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.nan.cloud.message.infrastructure.websocket.routing.SubscriptionLevel;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SubscribeFeedbackPayload {

    /**
     * 用户ID
     */
    private Long uid;

    /**
     * 订阅层次
     */
    private SubscriptionLevel subscriptionLevel;

    /**
     * 主题路径
     */
    private String topic;

    /**
     * 订阅/取消订阅是否成功
     */
    private boolean success;

    /**
     * 错误消息（失败时）
     */
    private String errorMsg;


    /**
     * 操作类型
     */
    private SubscribeOperation operation; // SUBSCRIBE, UNSUBSCRIBE


    public enum SubscribeOperation {

        SUBSCRIBE,

        UNSUBSCRIBE
    }
}
