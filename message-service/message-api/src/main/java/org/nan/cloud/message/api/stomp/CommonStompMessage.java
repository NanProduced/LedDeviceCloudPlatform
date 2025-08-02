package org.nan.cloud.message.api.stomp;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.nan.cloud.message.api.enums.Priority;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 面向前端SPA的STOMP消息结构体
 *
 * 设计特点：
 * 1. 扁平化结构，避免深度嵌套
 * 2. 语义化字段命名，前端易理解
 * 3. 最小化冗余，只保留必要字段
 * 4. 支持所有业务场景的统一格式
 *
 * @author Nan
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CommonStompMessage {

    // ==================== 消息标识 ====================

    /**
     * 消息唯一ID
     * 用于消息去重、ACK确认等
     */
    private String messageId;

    /**
     * 消息时间戳
     * 格式：ISO 8601 (2025-08-01T16:30:45.123Z)
     */
    private String timestamp;

    /**
     * 组织隔离
     */
    private Long oid;

    // ==================== 消息分类 ====================

    /**
     * 消息类型（主分类）
     * 枚举：COMMAND_FEEDBACK,TERMINAL_STATUS,TASK_PROGRESS...
     */
    private StompMessageTypes messageType;

    /* ==== 保留三个备用子类型 ==== */
    /**
     * 消息子类型（细分类）
     * 如：SUCCESS/FAILED（指令结果）, ONLINE/OFFLINE（终端状态）,
     *     DOWNLOAD/TRANSCODE（任务类型）等
     */
    private String subType_1;

    private String subType_2;

    private String subType_3;

    /**
     * 消息级别（影响前端显示样式）
     * 枚举：SUCCESS, INFO, WARNING, ERROR
     */
    private StompMessageLevel level;


    // ==================== 业务上下文 ====================

    /**
     * 消息上下文信息
     */
    private Context context;

    // ==================== 是否是聚合消息 ====================

    private Boolean aggregate =  Boolean.FALSE;

    // ==================== 消息内容 ====================

    /**
     * 消息标题
     * 用于前端列表显示和推送通知标题
     */
    private String title;

    /**
     * 消息描述
     * 用于前端详情显示和推送通知内容
     */
    private String content;

    /**
     * 业务数据载荷
     * 包含具体的业务数据，前端根据messageType解析
     */
    private Object payload;

    // ==================== 前端交互 ====================

    /**
     * 消息优先级
     * HIGH: 立即处理, NORMAL: 正常处理, LOW: 延迟处理
     */
    private Priority priority;

    /**
     * 是否需要用户确认
     * true: 需要用户点击确认, false: 自动处理
     */
    private Boolean requireAck;

    /**
     * 消息过期时间（毫秒）
     * 超过此时间消息将被自动丢弃
     */
    private Long ttl;

    /**
     * 可执行操作列表
     * 前端根据此字段显示操作按钮
     */
    private List<MessageAction> actions;



    // ==================== 扩展字段 ====================

    /**
     * 备用
     * 扩展属性
     * 用于特殊业务场景的自定义数据
     */
    private Map<String, Object> extra;

    /**
     * 统一使用规定的构造方法构造对应上下文信息
     */
    @Data
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Context {

        /**
         * 通过资源类型来决定填充哪些上下文信息
         */
        private StompResourceType resourceType;

        private Long uid;

        private Long tid;

        private String batchId;

        private String taskId;

        /**
         * 这里的CommandId是拼接后的字符串Id
         */
        private String commandId;


        /**
         * 指令上下文
         * @param uid 下发指令的用户
         * @param commandId 指令Id
         * @return
         */
        public static Context commandContext(Long uid, Long tid, String commandId) {
            return Context.builder()
                    .resourceType(StompResourceType.COMMAND)
                    .uid(uid)
                    .tid(tid)
                    .commandId(commandId)
                    .build();
        }

        /**
         * 批量指令上下文
         * @param uid 下发指令的用户
         * @param batchId 批量指令Id
         * @param commandId 单条执行指令Id
         * @return
         */
        public static Context batchCommandContext(Long uid, Long tid, String batchId, String commandId) {
            return Context.builder()
                    .resourceType(StompResourceType.BATCH)
                    .uid(uid)
                    .tid(tid)
                    .commandId(commandId)
                    .batchId(batchId)
                    .build();
        }

        /**
         * 任务上下文
         * @param uid 用户Id
         * @param taskId 任务Id
         * @return
         */
        public static Context taskContext(Long uid, String taskId) {
            return Context.builder()
                    .resourceType(StompResourceType.TASK)
                    .uid(uid)
                    .taskId(taskId)
                    .build();
        }

        /**
         * 终端上下文（带用户Id）
         * @param uid 和该消息相关的用户Id
         * @param tid 终端Id
         * @return
         */
        public static Context terminalContext(Long tid, Long uid) {
            return Context.builder()
                    .resourceType(StompResourceType.TERMINAL)
                    .tid(tid)
                    .uid(uid)
                    .build();
        }

        /**
         * 终端上下文
         * @param tid 终端Id
         * @return
         */
        public static Context terminalContext(Long tid) {
            return Context.builder()
                    .resourceType(StompResourceType.TERMINAL)
                    .tid(tid)
                    .build();
        }

    }

    /**
     * 消息操作定义
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class MessageAction {

        /**
         * 操作ID
         */
        private String actionId;

        /**
         * 操作显示名称
         */
        private String actionName;

        /**
         * 操作类型
         * 枚举：CONFIRM, RETRY, CANCEL, VIEW, DOWNLOAD, NAVIGATE
         */
        private String actionType;

        /**
         * 操作目标
         * URL路径、API端点或前端路由
         */
        private String actionTarget;

        /**
         * 操作参数
         */
        private Map<String, Object> parameters;
    }

}
