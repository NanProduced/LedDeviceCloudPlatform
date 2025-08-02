package org.nan.cloud.core.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nan.cloud.core.api.DTO.req.TopicPermissionRequest;
import org.nan.cloud.core.api.DTO.res.TopicPermissionResponse;
import org.nan.cloud.core.enums.UserTypeEnum;
import org.nan.cloud.core.repository.TerminalGroupRepository;
import org.nan.cloud.core.repository.UserGroupTerminalGroupBindingRepository;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * <p>STOMP权限验证处理</p>
 *
 * 提供STOMP主题订阅权限验证的核心业务逻辑。
 * 支持不同类型主题的权限验证，包括用户主题、组织主题、终端主题等。
 *
 * @author Nan
 * @since 1.0.0
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class StompTopicAuthHandler {

    private final TerminalGroupRepository terminalGroupRepository;
    private final UserGroupTerminalGroupBindingRepository bindingRepository;

    // 主题路径正则表达式
    private static final Pattern ORG_TOPIC_PATTERN = Pattern.compile("^/topic/org/(\\d+)");
    private static final Pattern DEVICE_TOPIC_PATTERN = Pattern.compile("^/topic/device/(.*)$");
    private static final Pattern TASK_TOPIC_PATTERN = Pattern.compile("^/topic/task/(.*)$");
    private static final Pattern BATCH_TOPIC_PATTERN = Pattern.compile("^/topic/batch/(.*)$");


    /**
     * 验证用户对STOMP主题的订阅权限
     *
     * 根据主题类型执行不同的权限验证逻辑：
     * - USER类型：验证是否为用户本人的主题
     * - ORG类型：验证是否为同组织用户
     * - TERMINAL类型：验证用户组是否有终端组权限
     * - SYSTEM类型：验证系统权限
     * - BATCH_COMMAND类型：验证批量操作权限
     *
     * @param request 权限验证请求
     * @return 权限验证结果
     */
    public TopicPermissionResponse verifyTopicSubscriptionPermission(TopicPermissionRequest request) {
        try {
            String topicPath = request.getTopicPath();
            String topicType = determineTopicType(topicPath, request.getTopicType());

            log.debug("开始验证STOMP主题权限 - 用户: {}, 组织: {}, 用户组: {}, 主题: {}, 类型: {}",
                    request.getUid(), request.getOid(), request.getUgid(), topicPath, topicType);

            boolean hasPermission = verifyTopicPermission(request, topicPath, topicType);

            if (hasPermission) {
                log.debug("✅ STOMP主题权限验证通过 - 用户: {}, 主题: {}", request.getUid(), topicPath);
                return TopicPermissionResponse.success();
            } else {
                String reason = String.format("用户无权限访问主题: %s (类型: %s)", topicPath, topicType);
                log.warn("❌ STOMP主题权限验证失败 - 用户: {}, 主题: {}, 原因: {}",
                        request.getUid(), topicPath, reason);
                return TopicPermissionResponse.denied(reason);
            }

        } catch (Exception e) {
            log.error("STOMP主题权限验证异常 - 用户: {}, 主题: {}, 错误: {}",
                    request.getUid(), request.getTopicPath(), e.getMessage(), e);
            return TopicPermissionResponse.denied("权限验证服务异常: " + e.getMessage());
        }
    }

    /**
     * 批量验证用户对多个STOMP主题的订阅权限
     *
     * 用于自动订阅场景，一次性验证多个主题的权限。
     * 返回每个主题的权限验证结果映射。
     *
     * @param request 批量权限验证请求
     * @return 包含每个主题权限验证结果的响应
     */
    public TopicPermissionResponse batchVerifyTopicSubscriptionPermission(TopicPermissionRequest request) {
        try {
            List<String> topicPaths = request.getTopicPaths();
            if (topicPaths == null || topicPaths.isEmpty()) {
                return TopicPermissionResponse.denied("批量验证请求中缺少主题列表");
            }

            log.debug("开始批量验证STOMP主题权限 - 用户: {}, 主题数量: {}",
                    request.getUid(), topicPaths.size());

            Map<String, TopicPermissionResponse.TopicPermissionResult> batchResults = new HashMap<>();

            for (String topicPath : topicPaths) {
                try {
                    String topicType = determineTopicType(topicPath, null);
                    boolean hasPermission = verifyTopicPermission(request, topicPath, topicType);

                    TopicPermissionResponse.TopicPermissionResult result = TopicPermissionResponse.TopicPermissionResult.builder()
                            .topicPath(topicPath)
                            .hasPermission(hasPermission)
                            .permissionLevel(hasPermission ? "READ" : "DENIED")
                            .deniedReason(hasPermission ? null : String.format("无权限访问主题: %s", topicPath))
                            .build();

                    batchResults.put(topicPath, result);

                    if (hasPermission) {
                        log.debug("✅ 批量权限验证通过 - 用户: {}, 主题: {}", request.getUid(), topicPath);
                    } else {
                        log.debug("❌ 批量权限验证失败 - 用户: {}, 主题: {}", request.getUid(), topicPath);
                    }

                } catch (Exception e) {
                    log.warn("批量验证中单个主题异常 - 主题: {}, 错误: {}", topicPath, e.getMessage());

                    TopicPermissionResponse.TopicPermissionResult result = TopicPermissionResponse.TopicPermissionResult.builder()
                            .topicPath(topicPath)
                            .hasPermission(false)
                            .permissionLevel("ERROR")
                            .deniedReason("权限验证异常: " + e.getMessage())
                            .build();

                    batchResults.put(topicPath, result);
                }
            }

            long successCount = batchResults.values().stream()
                    .mapToLong(result -> Boolean.TRUE.equals(result.getHasPermission()) ? 1 : 0)
                    .sum();

            log.info("✅ 批量STOMP主题权限验证完成 - 用户: {}, 总数: {}, 通过: {}",
                    request.getUid(), topicPaths.size(), successCount);

            return TopicPermissionResponse.success(batchResults);

        } catch (Exception e) {
            log.error("批量STOMP主题权限验证异常 - 用户: {}, 错误: {}", request.getUid(), e.getMessage(), e);
            return TopicPermissionResponse.denied("批量权限验证服务异常: " + e.getMessage());
        }
    }

    /**
     * 验证特定主题的权限
     */
    private boolean verifyTopicPermission(TopicPermissionRequest request, String topicPath, String topicType) {
        return switch (topicType) {
            case "ORG" -> verifyOrgTopicPermission(request, topicPath);
            case "DEVICE" -> verifyTerminalTopicPermission(request, topicPath);
            case "TASK" -> verifyTaskTopicPermission(request, topicPath);
            case "BATCH" -> verifyBatchAggTopicPermission(request, topicPath);
            default -> {
                log.warn("未知的主题类型: {} for 主题: {}", topicType, topicPath);
                yield false;
            }
        };
    }

    /**
     * 验证组织主题权限 - 同组织用户可访问
     */
    private boolean verifyOrgTopicPermission(TopicPermissionRequest request, String topicPath) {
        Matcher matcher = ORG_TOPIC_PATTERN.matcher(topicPath);
        if (!matcher.matches()) {
            log.warn("组织主题路径格式不正确: {}", topicPath);
            return false;
        }

        String topicOrgId = matcher.group(1);
        Long requestOrgId = request.getOid();

        // 用户只能访问自己组织的主题
        boolean hasPermission = requestOrgId != null && requestOrgId.toString().equals(topicOrgId);

        if (!hasPermission) {
            log.debug("组织主题权限验证失败 - 请求组织: {}, 主题组织: {}", requestOrgId, topicOrgId);
        }

        return hasPermission;
    }

    /**
     * 验证终端主题权限 - 需要检查用户组对终端组的权限绑定
     */
    private boolean verifyTerminalTopicPermission(TopicPermissionRequest request, String topicPath) {
        Matcher matcher = DEVICE_TOPIC_PATTERN.matcher(topicPath);
        if (!matcher.matches()) {
            log.warn("终端主题路径格式不正确: {}", topicPath);
            return false;
        }

        String terminalId = matcher.group(1);
        Long requestUgid = request.getUgid();
        Long requestOid = request.getOid();

        if (requestUgid == null || requestOid == null) {
            log.warn("终端主题权限验证缺少必要参数 - 用户组: {}, 组织: {}", requestUgid, requestOid);
            return false;
        }

        try {
            // 组织管理员跳过检查
            if (request.getUserType().equals(UserTypeEnum.ORG_MANAGER_USER.getCode())) {
                // 验证终端是否属于同一组织（通过终端组归属验证）
                return terminalGroupRepository.hasTerminalInOrg(requestOid, Long.parseLong(terminalId));
            }

            // 常规用户需要检查用户组对终端组的权限绑定
            return bindingRepository.hasPermissionForTerminal(requestUgid, Long.parseLong(terminalId));

        } catch (Exception e) {
            log.error("终端主题权限验证异常 - 终端: {}, 用户组: {}, 错误: {}",
                    terminalId, requestUgid, e.getMessage(), e);
            return false;
        }
    }

    /**
     * 验证任务主题权限
     * @param request
     * @param topicPath
     * @return
     */
    private boolean verifyTaskTopicPermission(TopicPermissionRequest request, String topicPath) {
        // todo
        return true;
    }

    /**
     * 验证批量指令主题权限 - 需要操作权限
     */
    private boolean verifyBatchAggTopicPermission(TopicPermissionRequest request, String topicPath) {
        // 批量指令主题需要特定的操作权限，目前允许所有已认证用户访问
        // 后续可以根据具体业务需求进行细化
        Long requestUid = request.getUid();
        Long requestOid = request.getOid();

        if (requestUid == null || requestOid == null) {
            log.warn("批量指令主题权限验证缺少必要参数 - 用户: {}, 组织: {}", requestUid, requestOid);
            return false;
        }

        // 基本的用户和组织有效性验证
        try {
            // todo: 待实现

            log.debug("批量指令主题权限验证通过 - 用户: {}", requestUid);
            return true;

        } catch (Exception e) {
            log.error("批量指令主题权限验证异常 - 用户: {}, 错误: {}", requestUid, e.getMessage(), e);
            return false;
        }
    }

    /**
     * 根据主题路径确定主题类型
     */
    private String determineTopicType(String topicPath, String providedType) {
        // 如果已提供明确的类型，优先使用
        if (StringUtils.hasText(providedType)) {
            return providedType;
        }

        // 根据路径模式推断类型
        if (ORG_TOPIC_PATTERN.matcher(topicPath).matches()) {
            return "ORG";
        } else if (DEVICE_TOPIC_PATTERN.matcher(topicPath).matches()) {
            return "DEVICE";
        } else if (TASK_TOPIC_PATTERN.matcher(topicPath).matches()) {
            return "TASK";
        } else if (BATCH_TOPIC_PATTERN.matcher(topicPath).matches()) {
            return "BATCH";
        } else {
            return "UNKNOWN";
        }
    }
}
