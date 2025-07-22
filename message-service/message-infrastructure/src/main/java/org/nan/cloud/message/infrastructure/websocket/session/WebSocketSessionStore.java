package org.nan.cloud.message.infrastructure.websocket.session;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * WebSocket会话存储器
 * 
 * 使用Redis存储WebSocket会话信息，实现会话持久化和跨节点共享。
 * 支持分布式部署场景下的会话管理。
 * 
 * 主要功能：
 * 1. 会话信息持久化：将会话数据存储到Redis
 * 2. 在线状态管理：维护用户在线状态
 * 3. 会话过期管理：自动清理过期会话
 * 4. 跨节点共享：支持多实例部署
 * 
 * Redis存储结构：
 * - ws:session:{sessionId} -> WebSocketSessionInfo (会话详细信息)
 * - ws:user:{userId} -> Set<sessionId> (用户的所有会话)
 * - ws:org:{orgId} -> Set<sessionId> (组织的所有会话)
 * - ws:online:users -> Set<userId> (在线用户集合)
 * 
 * @author Nan
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketSessionStore {
    
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    
    // Redis key前缀
    private static final String SESSION_KEY_PREFIX = "ws:session:";
    private static final String USER_SESSIONS_KEY_PREFIX = "ws:user:";
    private static final String ORG_SESSIONS_KEY_PREFIX = "ws:org:";
    private static final String ONLINE_USERS_KEY = "ws:online:users";
    
    // 会话过期时间（分钟）
    private static final long SESSION_EXPIRE_MINUTES = 30;
    
    /**
     * 保存会话信息
     * 
     * @param sessionInfo 会话信息
     */
    public void saveSession(WebSocketSessionInfo sessionInfo) {
        try {
            String sessionId = sessionInfo.getSessionId();
            String userId = sessionInfo.getUserId();
            String orgId = sessionInfo.getOrganizationId();
            
            log.debug("保存WebSocket会话到Redis - 用户: {}, 会话: {}", userId, sessionId);
            
            // 1. 保存会话详细信息
            String sessionKey = SESSION_KEY_PREFIX + sessionId;
            String sessionJson = objectMapper.writeValueAsString(sessionInfo);
            redisTemplate.opsForValue().set(sessionKey, sessionJson, SESSION_EXPIRE_MINUTES, TimeUnit.MINUTES);
            
            // 2. 添加到用户会话集合
            String userSessionsKey = USER_SESSIONS_KEY_PREFIX + userId;
            redisTemplate.opsForSet().add(userSessionsKey, sessionId);
            redisTemplate.expire(userSessionsKey, SESSION_EXPIRE_MINUTES, TimeUnit.MINUTES);
            
            // 3. 添加到组织会话集合
            String orgSessionsKey = ORG_SESSIONS_KEY_PREFIX + orgId;
            redisTemplate.opsForSet().add(orgSessionsKey, sessionId);
            redisTemplate.expire(orgSessionsKey, SESSION_EXPIRE_MINUTES, TimeUnit.MINUTES);
            
            // 4. 添加到在线用户集合
            redisTemplate.opsForSet().add(ONLINE_USERS_KEY, userId);
            
            log.debug("WebSocket会话保存成功 - 会话: {}", sessionId);
            
        } catch (Exception e) {
            log.error("保存WebSocket会话失败 - 会话: {}, 错误: {}", 
                    sessionInfo.getSessionId(), e.getMessage(), e);
        }
    }
    
    /**
     * 获取会话信息
     * 
     * @param sessionId 会话ID
     * @return 会话信息，不存在时返回null
     */
    public WebSocketSessionInfo getSession(String sessionId) {
        try {
            String sessionKey = SESSION_KEY_PREFIX + sessionId;
            String sessionJson = redisTemplate.opsForValue().get(sessionKey);
            
            if (sessionJson == null) {
                return null;
            }
            
            return objectMapper.readValue(sessionJson, WebSocketSessionInfo.class);
            
        } catch (Exception e) {
            log.error("获取WebSocket会话失败 - 会话: {}, 错误: {}", sessionId, e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * 移除会话信息
     * 
     * @param sessionId 会话ID
     */
    public void removeSession(String sessionId) {
        try {
            log.debug("从Redis移除WebSocket会话 - 会话: {}", sessionId);
            
            // 1. 获取会话信息
            WebSocketSessionInfo sessionInfo = getSession(sessionId);
            if (sessionInfo == null) {
                log.debug("会话不存在，无需移除 - 会话: {}", sessionId);
                return;
            }
            
            String userId = sessionInfo.getUserId();
            String orgId = sessionInfo.getOrganizationId();
            
            // 2. 删除会话详细信息
            String sessionKey = SESSION_KEY_PREFIX + sessionId;
            redisTemplate.delete(sessionKey);
            
            // 3. 从用户会话集合中移除
            String userSessionsKey = USER_SESSIONS_KEY_PREFIX + userId;
            redisTemplate.opsForSet().remove(userSessionsKey, sessionId);
            
            // 4. 从组织会话集合中移除
            String orgSessionsKey = ORG_SESSIONS_KEY_PREFIX + orgId;
            redisTemplate.opsForSet().remove(orgSessionsKey, sessionId);
            
            // 5. 检查用户是否还有其他会话，没有则从在线用户集合中移除
            Set<String> userSessions = redisTemplate.opsForSet().members(userSessionsKey);
            if (userSessions == null || userSessions.isEmpty()) {
                redisTemplate.opsForSet().remove(ONLINE_USERS_KEY, userId);
                log.debug("用户完全离线 - 用户: {}", userId);
            }
            
            log.debug("WebSocket会话移除成功 - 会话: {}", sessionId);
            
        } catch (Exception e) {
            log.error("移除WebSocket会话失败 - 会话: {}, 错误: {}", sessionId, e.getMessage(), e);
        }
    }
    
    /**
     * 获取用户的所有会话ID
     * 
     * @param userId 用户ID
     * @return 用户的所有会话ID集合
     */
    public Set<String> getUserSessions(String userId) {
        String userSessionsKey = USER_SESSIONS_KEY_PREFIX + userId;
        return redisTemplate.opsForSet().members(userSessionsKey);
    }
    
    /**
     * 获取组织的所有会话ID
     * 
     * @param organizationId 组织ID
     * @return 组织的所有会话ID集合
     */
    public Set<String> getOrganizationSessions(String organizationId) {
        String orgSessionsKey = ORG_SESSIONS_KEY_PREFIX + organizationId;
        return redisTemplate.opsForSet().members(orgSessionsKey);
    }
    
    /**
     * 获取所有在线用户ID
     * 
     * @return 在线用户ID集合
     */
    public Set<String> getOnlineUsers() {
        return redisTemplate.opsForSet().members(ONLINE_USERS_KEY);
    }
    
    /**
     * 检查用户是否在线
     * 
     * @param userId 用户ID
     * @return true表示在线，false表示离线
     */
    public boolean isUserOnline(String userId) {
        return redisTemplate.opsForSet().isMember(ONLINE_USERS_KEY, userId);
    }
    
    /**
     * 获取在线用户数量
     * 
     * @return 在线用户数量
     */
    public long getOnlineUserCount() {
        return redisTemplate.opsForSet().size(ONLINE_USERS_KEY);
    }
    
    /**
     * 更新会话最后活跃时间
     * 
     * @param sessionId 会话ID
     */
    public void updateSessionActivity(String sessionId) {
        try {
            WebSocketSessionInfo sessionInfo = getSession(sessionId);
            if (sessionInfo != null) {
                sessionInfo.setLastActivityTime(LocalDateTime.now());
                saveSession(sessionInfo);
            }
        } catch (Exception e) {
            log.error("更新会话活跃时间失败 - 会话: {}, 错误: {}", sessionId, e.getMessage(), e);
        }
    }
    
    /**
     * 获取所有会话ID
     * 用于心跳检测和统计
     * 
     * @return 所有活跃会话ID集合
     */
    public Set<String> getAllSessionIds() {
        try {
            String pattern = SESSION_KEY_PREFIX + "*";
            Set<String> keys = redisTemplate.keys(pattern);
            
            if (keys == null) {
                return Set.of();
            }
            
            // 提取会话ID（移除前缀）
            return keys.stream()
                    .map(key -> key.substring(SESSION_KEY_PREFIX.length()))
                    .collect(java.util.stream.Collectors.toSet());
                    
        } catch (Exception e) {
            log.error("获取所有会话ID失败: {}", e.getMessage(), e);
            return Set.of();
        }
    }

    /**
     * 清理过期会话
     * 定期调用此方法清理Redis中的过期数据
     */
    public void cleanupExpiredSessions() {
        try {
            log.info("开始清理过期WebSocket会话");
            
            // 获取所有在线用户
            Set<String> onlineUsers = getOnlineUsers();
            int cleanupCount = 0;
            
            for (String userId : onlineUsers) {
                Set<String> userSessions = getUserSessions(userId);
                
                for (String sessionId : userSessions) {
                    WebSocketSessionInfo sessionInfo = getSession(sessionId);
                    
                    // 检查会话是否过期（超过30分钟无活动）
                    if (sessionInfo == null || isSessionExpired(sessionInfo)) {
                        removeSession(sessionId);
                        cleanupCount++;
                    }
                }
            }
            
            log.info("过期会话清理完成 - 清理数量: {}", cleanupCount);
            
        } catch (Exception e) {
            log.error("清理过期会话失败: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 检查会话是否过期
     * 
     * @param sessionInfo 会话信息
     * @return true表示过期，false表示未过期
     */
    private boolean isSessionExpired(WebSocketSessionInfo sessionInfo) {
        if (sessionInfo.getLastActivityTime() == null) {
            return false; // 没有活跃时间记录，不认为过期
        }
        
        LocalDateTime expireTime = sessionInfo.getLastActivityTime().plusMinutes(SESSION_EXPIRE_MINUTES);
        return LocalDateTime.now().isAfter(expireTime);
    }
}