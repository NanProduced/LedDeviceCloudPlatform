package org.nan.cloud.terminal.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nan.cloud.common.basic.utils.JsonUtils;
import org.nan.cloud.terminal.api.common.model.TerminalCommand;
import org.nan.cloud.terminal.api.common.utils.CommandIdGenerator;
import org.nan.cloud.terminal.mq.producer.CommandConfirmationMessageService;
import org.nan.cloud.terminal.config.properties.TerminalInfrastructureProperties;
import org.nan.cloud.terminal.infrastructure.config.RedisConfig;
import org.nan.cloud.terminal.infrastructure.connection.ShardedConnectionManager;
import org.nan.cloud.terminal.websocket.model.WebsocketTerminalCommand;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.time.Duration;
import java.util.*;

/**
 *  指令管理<p>
 *  - Redis Sorted Set：O(log N)的指令插入和查询<p>
 *  - 原子性操作：使用Redis事务保证数据一致性<p>
 *  - 分片存储：按oid:tid分片，避免单点热点<p>
 *  - 智能去重：author_url维度的去重<p>
 *  - 队列限制：最大100条指令，LRU淘汰策略
 * @author Nan
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class TerminalCommandManager {

    // Redis三层数据结构
    // 1. 待执行指令队列：terminal:cmd:queue:{oid}:{tid}  (Sorted Set)
    // 2. 去重索引映射：terminal:cmd:dedup:{oid}:{tid}    (Hash: author_url -> command_id)
    // 3. 指令详情缓存：terminal:cmd:detail:{command_id} (String: JSON)

    private final StringRedisTemplate stringRedisTemplate;
    private final TerminalOnlineStatusManager onlineStatusManager;
    private final ShardedConnectionManager connectionManager;
    private final TerminalInfrastructureProperties terminalInfrastructureProperties;
    private final CommandConfirmationMessageService commandConfirmationMessageService;

    /**
     * 智能指令去重和存储
     * - 相同author_url的指令只保留最新的
     * - 使用Redis事务保证原子性
     */
    public Integer saveCommandWithDeduplication(Long oid, Long tid, TerminalCommand command) {
        final Integer commandId = CommandIdGenerator.generateId();
        command.setId(commandId);

        String queueKey = String.format(RedisConfig.RedisKeys.COMMAND_QUEUE_PATTERN, oid, tid);
        String dedupKey = String.format(RedisConfig.RedisKeys.COMMAND_DEDUPLICATION_PATTERN, oid, tid);
        String detailKey = String.format(RedisConfig.RedisKeys.COMMAND_DETAIL_PATTERN, oid, tid, commandId);

        int timeout = terminalInfrastructureProperties.getCommand().getQueue_timeout();

        // Redis事务执行去重逻辑
        // 检查是否存在相同author_url的指令
        Object existingCommandIdObj = stringRedisTemplate.opsForHash().get(dedupKey, command.getAuthorUrl());
        Integer existingCommandId = existingCommandIdObj != null ? Integer.valueOf(existingCommandIdObj.toString()) : null;

        long currentTime = System.currentTimeMillis();

        // 执行去重和更新逻辑
        stringRedisTemplate.execute(new SessionCallback<List<Object>>() {
            @Override
            public List<Object> execute(RedisOperations operations) throws DataAccessException {
                operations.multi();

                // 如果存在旧指令，先移除
                if (existingCommandId != null) {
                    operations.opsForZSet().remove(queueKey, existingCommandId.toString());
                    String oldDetailKey = String.format(RedisConfig.RedisKeys.COMMAND_DETAIL_PATTERN, oid, tid, existingCommandId);
                    operations.delete(oldDetailKey);

                    log.info("替换重复指令: oid={}, tid={}, author_url={}, old_id={}, new_id={}",
                            oid, tid, command.getAuthorUrl(), existingCommandId, commandId);
                }

                // 添加新指令（使用commandId作为ZSet成员）
                operations.opsForZSet().add(queueKey, commandId.toString(), currentTime);
                operations.opsForHash().put(dedupKey, command.getAuthorUrl(), commandId.toString());
                operations.opsForValue().set(detailKey, JsonUtils.toJson(command), Duration.ofHours(timeout));

                // 设置过期时间
                operations.expire(queueKey, Duration.ofHours(timeout));
                operations.expire(dedupKey, Duration.ofHours(timeout));

                return operations.exec();
            }
        });

        log.info("指令保存成功: oid={}, tid={}, commandId={}, author_url={}",
                oid, tid, commandId, command.getAuthorUrl());

        return commandId;
    }

    /**
     * 指令下发
     */
    public TerminalCommand sendCommand(Long oid, Long tid, TerminalCommand command) {
        Integer commandId = saveCommandWithDeduplication(oid, tid, command);

        // 检查终端在线状态
        if (onlineStatusManager.isTerminalOnline(oid, tid)) {
            // 在线：WebSocket实时下发
            boolean sent = sendViaWebSocket(oid, tid, command);
            if (sent) {
                log.info("WebSocket指令下发成功: oid={}, tid={}, commandId={}", oid, tid, commandId);
                markCommandsAsSent(oid, tid, Collections.singletonList(command));
                return command;
            }
        }
        // 离线或WebSocket下发失败：等待HTTP拉取
        updateCommandStatus(oid, tid, commandId, CommandStatus.PENDING);
        log.debug("指令已保存等待拉取: oid={}, tid={}, commandId={}", oid, tid, commandId);
        return command;
    }

    /**
     * HTTP拉取指令接口
     */
    public List<TerminalCommand> getPendingCommands(Long oid, Long tid) {
        String queueKey = String.format(RedisConfig.RedisKeys.COMMAND_QUEUE_PATTERN, oid, tid);

        // 获取所有待执行指令ID（按时间排序）
        Set<String> commandIdStrs = stringRedisTemplate.opsForZSet().range(queueKey, 0, -1);
        List<TerminalCommand> commands = new ArrayList<>();

        if (CollectionUtils.isEmpty(commandIdStrs)) return commands;

        for (String commandIdStr : commandIdStrs) {
            Integer commandId = Integer.valueOf(commandIdStr);
            String detailKey = String.format(RedisConfig.RedisKeys.COMMAND_DETAIL_PATTERN, oid, tid, commandId);
            String commandJson = stringRedisTemplate.opsForValue().get(detailKey);

            if (commandJson != null) {
                TerminalCommand command = JsonUtils.fromJson(commandJson, TerminalCommand.class);
                commands.add(command);
            }
        }
        markCommandsAsSent(oid, tid, commands);
        log.info("HTTP拉取指令: oid={}, tid={}, count={}", oid, tid, commands.size());
        return commands;
    }

    /**
     * 指令确认处理
     */
    public boolean confirmCommand(Long oid, Long tid, Integer commandId, String result) {
        // 验证指令是否存在
        String detailKey = String.format(RedisConfig.RedisKeys.COMMAND_DETAIL_PATTERN, oid, tid, commandId);
        String commandJson = stringRedisTemplate.opsForValue().get(detailKey);

        if (commandJson == null) {
            log.warn("指令不存在: oid={}, tid={}, commandId={}", oid, tid, commandId);
            return false;
        }

        // 处理确认结果
        if ("Executable comment".equals(result)) {
            // 指令可执行
            handleCommandExecution(oid, tid, commandId);
            return true;
        } else {
            // 指令不可执行
            handleCommandRejection(oid, tid, commandId);
            return false;
        }
    }

    private void handleCommandExecution(Long oid, Long tid, Integer commandId) {
        // 获取指令详情
        String detailKey = String.format(RedisConfig.RedisKeys.COMMAND_DETAIL_PATTERN, oid, tid, commandId);
        String commandJson = stringRedisTemplate.opsForValue().get(detailKey);
        TerminalCommand command = null;
        
        if (commandJson != null) {
            command = JsonUtils.fromJson(commandJson, TerminalCommand.class);
        }
        
        // 更新指令状态为已执行
        updateCommandStatus(oid, tid, commandId, CommandStatus.EXECUTED);
        // 从待执行队列中移除
        removeCommandFromQueue(oid, tid, commandId);
        
        // 发送指令执行成功的MQ消息
        if (command != null && command.getUid() != null) {
            try {
                commandConfirmationMessageService.sendCommandExecutionSuccessAsync(
                        oid, tid, commandId, command, command.getUid());
                log.info("✅ 指令执行成功消息已发送 - oid: {}, tid: {}, commandId: {}, userId: {}", 
                        oid, tid, commandId, command.getUid());
            } catch (Exception e) {
                log.error("发送指令执行成功消息异常 - oid: {}, tid: {}, commandId: {}, 错误: {}", 
                        oid, tid, commandId, e.getMessage(), e);
            }
        } else {
            log.warn("无法发送指令执行成功消息 - 指令信息不完整: oid={}, tid={}, commandId={}", 
                    oid, tid, commandId);
        }
        
        log.info("指令执行确认: oid={}, tid={}, commandId={}", oid, tid, commandId);
    }

    private void handleCommandRejection(Long oid, Long tid, Integer commandId) {
        // 获取指令详情
        String detailKey = String.format(RedisConfig.RedisKeys.COMMAND_DETAIL_PATTERN, oid, tid, commandId);
        String commandJson = stringRedisTemplate.opsForValue().get(detailKey);
        TerminalCommand command = null;
        
        if (commandJson != null) {
            command = JsonUtils.fromJson(commandJson, TerminalCommand.class);
        }
        
        updateCommandStatus(oid, tid, commandId, CommandStatus.REJECTED);
        removeCommandFromQueue(oid, tid, commandId);
        
        // 发送指令被拒绝的MQ消息
        if (command != null && command.getUid() != null) {
            try {
                commandConfirmationMessageService.sendCommandRejectionAsync(
                        oid, tid, commandId, command, command.getUid());
                log.info("✅ 指令拒绝消息已发送 - oid: {}, tid: {}, commandId: {}, userId: {}",
                        oid, tid, commandId, command.getUid());
            } catch (Exception e) {
                log.error("发送指令拒绝消息异常 - oid: {}, tid: {}, commandId: {}, 错误: {}", 
                        oid, tid, commandId, e.getMessage(), e);
            }
        } else {
            log.warn("无法发送指令拒绝消息 - 指令信息不完整: oid={}, tid={}, commandId={}", 
                    oid, tid, commandId);
        }
        
        log.info("指令被拒绝: oid={}, tid={}, commandId={}", oid, tid, commandId);
    }

    private void updateCommandStatus(Long oid, Long tid, Integer commandId, CommandStatus status) {
        // 可以使用Redis Hash存储指令状态
        String statusKey = String.format("terminal:cmd:status:%d:%d", oid, tid);
        stringRedisTemplate.opsForHash().put(statusKey, commandId.toString(), status.name());
        stringRedisTemplate.expire(statusKey, Duration.ofHours(terminalInfrastructureProperties.getCommand().getStatus_timeout()));
    }

    private void removeCommandFromQueue(Long oid, Long tid, Integer commandId) {
        String queueKey = String.format(RedisConfig.RedisKeys.COMMAND_QUEUE_PATTERN, oid, tid);
        String dedupKey = String.format(RedisConfig.RedisKeys.COMMAND_DEDUPLICATION_PATTERN, oid, tid);
        String detailKey = String.format(RedisConfig.RedisKeys.COMMAND_DETAIL_PATTERN, oid, tid, commandId);

        // 获取指令信息以便从去重索引中移除
        String commandJson = stringRedisTemplate.opsForValue().get(detailKey);
        if (commandJson != null) {
            TerminalCommand command = JsonUtils.fromJson(commandJson, TerminalCommand.class);

            // 批量删除
            stringRedisTemplate.execute(new SessionCallback<List<Object>>() {
                @Override
                public List<Object> execute(RedisOperations operations) throws DataAccessException {
                    operations.multi();

                    operations.opsForZSet().remove(queueKey, commandId.toString());
                    operations.opsForHash().delete(dedupKey, command.getAuthorUrl());
                    operations.delete(detailKey);

                    return operations.exec();
                }
            });
        }
    }

    private void markCommandsAsSent(Long oid, Long tid, List<TerminalCommand> commands) {
        for (TerminalCommand command : commands) {
            updateCommandStatus(oid, tid, command.getId(), CommandStatus.SENT);
        }
    }

    private boolean sendViaWebSocket(Long oid, Long tid, TerminalCommand command) {
        // 构造WebSocket消息格式
        WebsocketTerminalCommand wsCommand = new WebsocketTerminalCommand(command, tid.intValue());

        return connectionManager.sendMessage(oid, tid, JsonUtils.toJson(wsCommand));
    }

    /**
     * 指令状态枚举
     */
    public enum CommandStatus {
        PENDING,    // 待下发
        SENT,       // 已下发
        EXECUTED,   // 已执行
        REJECTED    // 被拒绝
    }
}

