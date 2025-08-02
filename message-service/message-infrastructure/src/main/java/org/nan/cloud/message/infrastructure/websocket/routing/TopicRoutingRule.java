package org.nan.cloud.message.infrastructure.websocket.routing;

import lombok.Builder;
import lombok.Data;
import org.nan.cloud.message.api.stomp.CommonStompMessage;

import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * 主题路由规则
 * 
 * 定义了如何根据消息内容决定路由到哪些主题的规则。
 * 每个规则包含匹配条件和主题生成逻辑。
 * 
 * @author Nan
 * @since 1.0.0
 */
@Data
@Builder
public class TopicRoutingRule {
    
    /**
     * 规则名称，用于日志和调试
     */
    private String ruleName;
    
    /**
     * 规则优先级，数字越小优先级越高
     */
    private int priority;
    
    /**
     * 消息匹配器，决定该规则是否适用于给定消息
     */
    private Predicate<CommonStompMessage> matcher;
    
    /**
     * 主题生成器，根据消息内容生成目标主题列表
     */
    private Function<CommonStompMessage, List<String>> topicGenerator;
    
    /**
     * 检查消息是否匹配此规则
     * 
     * @param message STOMP消息
     * @return true表示匹配，false表示不匹配
     */
    public boolean matches(CommonStompMessage message) {
        return matcher != null && matcher.test(message);
    }
    
    /**
     * 应用路由规则，生成路由决策
     * 
     * @param message STOMP消息
     * @return 路由决策结果
     */
    public TopicRoutingDecision apply(CommonStompMessage message) {
        List<String> targetTopics = topicGenerator != null ? 
                topicGenerator.apply(message) : List.of();
        
        return TopicRoutingDecision.builder()
                .messageId(message.getMessageId())
                .routingStrategy(RoutingStrategy.RULE_BASED)
                .targetTopics(targetTopics)
                .appliedRule(this.ruleName)
                .build();
    }
}