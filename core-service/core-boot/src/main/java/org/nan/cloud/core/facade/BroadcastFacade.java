package org.nan.cloud.core.facade;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nan.cloud.common.basic.exception.BaseException;
import org.nan.cloud.common.basic.exception.ExceptionEnum;
import org.nan.cloud.common.basic.model.PageRequestDTO;
import org.nan.cloud.common.basic.model.PageVO;
import org.nan.cloud.common.web.context.InvocationContextHolder;
import org.nan.cloud.common.web.context.RequestUserInfo;
import org.nan.cloud.core.api.DTO.req.QueryBroadcastMessageRequest;
import org.nan.cloud.core.api.DTO.res.BroadcastMessageResponse;
import org.nan.cloud.core.service.BroadcastMessageService;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 广播消息门面类
 * 
 * 负责处理广播消息相关的业务逻辑协调
 * 符合项目现有的Facade层设计模式
 * 
 * @author LedDeviceCloudPlatform Team
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BroadcastFacade {

    private final BroadcastMessageService broadcastMessageService;

    /**
     * 获取用户广播消息列表
     * 
     * @param requestDTO 分页查询请求
     * @return 分页消息结果
     */
    public PageVO<BroadcastMessageResponse> getUserBroadcastMessages(PageRequestDTO<QueryBroadcastMessageRequest> requestDTO) {
        try {
            RequestUserInfo requestUser = InvocationContextHolder.getContext().getRequestUser();
            Long userId = requestUser.getUid();
            Long orgId = requestUser.getOid();
            
            log.debug("获取用户广播消息列表 - 用户ID: {}, 组织ID: {}", userId, orgId);
            
            return broadcastMessageService.getUserBroadcastMessages(requestDTO, userId, orgId);
            
        } catch (BaseException e) {
            throw e;
        } catch (Exception e) {
            log.error("获取用户广播消息列表失败 - 错误: {}", e.getMessage(), e);
            throw new BaseException(ExceptionEnum.SERVER_ERROR, "获取广播消息列表失败");
        }
    }

    /**
     * 获取用户未读广播消息总数
     * 
     * @return 未读消息数量
     */
    public Long getUnreadCount() {
        try {
            RequestUserInfo requestUser = InvocationContextHolder.getContext().getRequestUser();
            Long userId = requestUser.getUid();
            Long orgId = requestUser.getOid();
            
            log.debug("获取用户未读广播消息数量 - 用户ID: {}, 组织ID: {}", userId, orgId);
            
            return broadcastMessageService.getUnreadCount(userId, orgId);
            
        } catch (Exception e) {
            log.error("获取用户未读广播消息数量失败 - 错误: {}", e.getMessage(), e);
            // 未读消息数量查询异常时返回0，不影响前端正常显示
            return 0L;
        }
    }

    /**
     * 获取用户按消息类型分组的未读广播消息数量
     * 
     * @return 按消息类型分组的未读数量映射
     */
    public Map<String, Long> getUnreadCountByType() {
        try {
            RequestUserInfo requestUser = InvocationContextHolder.getContext().getRequestUser();
            Long userId = requestUser.getUid();
            Long orgId = requestUser.getOid();
            
            log.debug("获取用户按类型未读广播消息数量 - 用户ID: {}, 组织ID: {}", userId, orgId);
            
            return broadcastMessageService.getUnreadCountByType(userId, orgId);
            
        } catch (Exception e) {
            log.error("获取用户按类型未读广播消息数量失败 - 错误: {}", e.getMessage(), e);
            // 异常时返回空Map，不影响前端正常显示
            return Map.of();
        }
    }

    /**
     * 获取广播消息详情
     * 
     * @param messageId 消息ID
     * @return 消息详情（包含已读状态）
     */
    public BroadcastMessageResponse getMessageDetail(String messageId) {
        try {
            RequestUserInfo requestUser = InvocationContextHolder.getContext().getRequestUser();
            Long userId = requestUser.getUid();
            
            log.debug("获取广播消息详情 - 消息ID: {}, 用户ID: {}", messageId, userId);
            
            return broadcastMessageService.getMessageById(messageId, userId);
            
        } catch (BaseException e) {
            throw e;
        } catch (Exception e) {
            log.error("获取广播消息详情失败 - 消息ID: {}, 错误: {}", messageId, e.getMessage(), e);
            throw new BaseException(ExceptionEnum.SERVER_ERROR, "获取消息详情失败");
        }
    }
}