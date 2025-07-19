package org.nan.cloud.core.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nan.cloud.common.basic.model.BindingType;
import org.nan.cloud.core.DTO.*;
import org.nan.cloud.core.domain.TerminalGroup;
import org.nan.cloud.core.domain.UserGroupTerminalGroupBinding;
import org.nan.cloud.core.repository.UserGroupTerminalGroupBindingRepository;
import org.nan.cloud.core.service.TerminalGroupService;
import org.nan.cloud.core.service.UserGroupTerminalGroupBindingService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StopWatch;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserGroupTerminalGroupBindingServiceImpl implements UserGroupTerminalGroupBindingService {

    private final UserGroupTerminalGroupBindingRepository bindingRepository;
    private final TerminalGroupService terminalGroupService;


    @Override
    public List<Long> getAccessibleTerminalGroupIds(Long ugid) {
        // 获取用户组的所有绑定关系
        List<UserGroupTerminalGroupBinding> allBindings = bindingRepository.getUserGroupBindings(ugid);
        
        // 计算包含的终端组ID集合
        Set<Long> includedTerminalGroupIds = new HashSet<>();
        Set<Long> excludedTerminalGroupIds = new HashSet<>();
        
        // 处理包含绑定
        for (UserGroupTerminalGroupBinding binding : allBindings) {
            if (binding.getBindingType() == BindingType.INCLUDE) {
                includedTerminalGroupIds.add(binding.getTgid());
                
                // 如果包含子组，添加所有子组
                if (binding.getIncludeSub()) {
                    List<TerminalGroup> childGroups = terminalGroupService.getChildGroups(binding.getTgid());
                    for (TerminalGroup childGroup : childGroups) {
                        includedTerminalGroupIds.add(childGroup.getTgid());
                    }
                }
            }
        }
        
        // 处理排除绑定
        for (UserGroupTerminalGroupBinding binding : allBindings) {
            if (binding.getBindingType() == BindingType.EXCLUDE) {
                excludedTerminalGroupIds.add(binding.getTgid());
                
                // 如果排除包含子组，排除所有子组
                if (binding.getIncludeSub()) {
                    List<TerminalGroup> childGroups = terminalGroupService.getChildGroups(binding.getTgid());
                    for (TerminalGroup childGroup : childGroups) {
                        excludedTerminalGroupIds.add(childGroup.getTgid());
                    }
                }
            }
        }
        
        // 计算最终权限：包含权限 - 排除权限
        includedTerminalGroupIds.removeAll(excludedTerminalGroupIds);
        
        return new ArrayList<>(includedTerminalGroupIds);
    }

    
    /**
     * 判断一个终端组是否是另一个终端组的子组
     */
    private boolean isChildOf(TerminalGroup child, TerminalGroup parent) {
        if (child == null || parent == null || child.getPath() == null) {
            return false;
        }
        
        // 通过路径判断父子关系
        String childPath = child.getPath();
        String parentPath = parent.getPath();
        
        // 子组的路径应该包含父组的路径
        return childPath.startsWith(parentPath + "|") || childPath.contains("|" + parent.getTgid() + "|");
    }

    @Override
    @Transactional
    public PermissionExpressionResultDTO updatePermissionExpression(PermissionExpressionDTO request) {
        LocalDateTime operationTime = LocalDateTime.now();
        StopWatch updateWatch = new StopWatch();
        updateWatch.start();
        // 记录操作开始日志
        log.info("[权限表达式更新] 开始处理权限表达式更新请求 - 用户组ID: {}, 操作者ID: {}, 绑定数量: {}, 冗余优化: {}", 
                request.getUgid(), request.getOperatorId(), 
                request.getPermissionBindings() != null ? request.getPermissionBindings().size() : 0,
                request.getEnableRedundancyOptimization());
        
        // 获取当前用户组的所有绑定关系
        List<UserGroupTerminalGroupBinding> currentBindings = bindingRepository.getUserGroupBindings(request.getUgid());
        int originalCount = currentBindings.size();
        
        log.info("[权限表达式更新] 当前用户组绑定状态 - 用户组ID: {}, 现有绑定数量: {}", 
                request.getUgid(), originalCount);
        
        // 转换权限表达式为Domain对象
        List<UserGroupTerminalGroupBinding> newBindings = convertToBindings(request);
        
        // 智能冗余清理
        List<UserGroupTerminalGroupBinding> optimizedBindings = newBindings;
        int redundancyRemoved = 0;
        
        if (request.getEnableRedundancyOptimization()) {
            log.info("[权限表达式更新] 开始执行智能冗余清理 - 用户组ID: {}, 原始绑定数量: {}", 
                    request.getUgid(), newBindings.size());
            
            PermissionOptimizationResult optimizationResult = optimizePermissionBindings(newBindings);
            optimizedBindings = optimizationResult.getOptimizedBindings();
            redundancyRemoved = optimizationResult.getRedundancyRemoved();
            
            log.info("[权限表达式更新] 智能冗余清理完成 - 用户组ID: {}, 优化后绑定数量: {}, 移除冗余: {}", 
                    request.getUgid(), optimizedBindings.size(), redundancyRemoved);
        }
        
        // 执行全量替换操作
        try {
            log.info("[权限表达式更新] 开始执行数据库全量替换 - 用户组ID: {}, 替换绑定数量: {}", 
                    request.getUgid(), optimizedBindings.size());
            
            bindingRepository.replaceUserGroupPermissions(request.getUgid(), optimizedBindings);
            
            // 构建操作统计信息
            PermissionExpressionResultDTO.OperationStatisticsDTO statistics = buildOperationStatistics(
                    originalCount, optimizedBindings.size(), redundancyRemoved, currentBindings, optimizedBindings);
            
            // 记录成功日志
            log.info("[权限表达式更新] 权限表达式更新成功 - 用户组ID: {}, 操作者ID: {}, 原始绑定: {}, 优化后绑定: {}, 冗余清理: {}, 优化率: {}%", 
                    request.getUgid(), request.getOperatorId(), statistics.getOriginalCount(), 
                    statistics.getOptimizedCount(), statistics.getRedundancyRemoved(), 
                    String.format("%.2f", statistics.getOptimizationRatio()));
            
            // 记录详细的操作变更
            logOperationDetails(request.getUgid(), currentBindings, optimizedBindings);
            
            // 构建响应
            return PermissionExpressionResultDTO.builder()
                    .success(true)
                    .message("权限表达式更新成功")
                    .ugid(request.getUgid())
                    .statistics(statistics)
                    .optimizedBindings(convertToOptimizedBindings(optimizedBindings))
                    .operationDetails(buildOperationDetails(currentBindings, optimizedBindings))
                    .operationTime(operationTime)
                    .build();
                    
        } catch (Exception e) {
            // 记录失败日志
            log.error("[权限表达式更新] 权限表达式更新失败 - 用户组ID: {}, 操作者ID: {}, 错误信息: {}", 
                    request.getUgid(), request.getOperatorId(), e.getMessage(), e);
            return PermissionExpressionResultDTO.builder()
                    .success(false)
                    .message("权限表达式更新失败: " + e.getMessage())
                    .ugid(request.getUgid())
                    .operationTime(operationTime)
                    .build();
        } finally {
            updateWatch.stop();
            log.info("[权限表达式更新] - 耗时： {}", updateWatch.getTotalTimeMillis());
        }
    }

    @Override
    public UserGroupPermissionStatusDTO getUserGroupPermissionStatus(Long ugid) {
        // 获取详细的权限绑定信息
        List<UserGroupTerminalGroupBinding> bindings = bindingRepository.getUserGroupPermissionDetails(ugid);
        
        // 转换为DTO
        List<UserGroupPermissionStatusDTO.PermissionBindingStatusDTO> bindingStatuses = 
                convertToPermissionBindingStatuses(bindings);
        
        // 计算统计信息
        UserGroupPermissionStatusDTO.BindingStatisticsDTO statistics = calculateBindingStatistics(bindings);
        
        // 获取最后更新时间
        LocalDateTime lastUpdateTime = bindings.stream()
                .map(UserGroupTerminalGroupBinding::getUpdateTime)
                .filter(Objects::nonNull)
                .max(LocalDateTime::compareTo)
                .orElse(null);
        
        return UserGroupPermissionStatusDTO.builder()
                .ugid(ugid)
                .permissionBindings(bindingStatuses)
                .statistics(statistics)
                .lastUpdateTime(lastUpdateTime)
                .build();
    }

    /**
     * 转换权限表达式DTO为Domain对象
     */
    private List<UserGroupTerminalGroupBinding> convertToBindings(PermissionExpressionDTO request) {
        return request.getPermissionBindings().stream()
                .map(bindingDTO -> UserGroupTerminalGroupBinding.builder()
                        .ugid(request.getUgid())
                        .tgid(bindingDTO.getTgid())
                        .bindingType(bindingDTO.getBindingType())
                        .includeSub(bindingDTO.getIncludeChildren())
                        .oid(request.getOid())
                        .creatorId(request.getOperatorId())
                        .createTime(LocalDateTime.now())
                        .updaterId(request.getOperatorId())
                        .updateTime(LocalDateTime.now())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * 智能权限优化算法
     */
    private PermissionOptimizationResult optimizePermissionBindings(List<UserGroupTerminalGroupBinding> bindings) {
        StopWatch optimizeWatch = new StopWatch();
        optimizeWatch.start();
        // 按层级深度排序，确保从根到叶的优先级
        Map<Long, TerminalGroup> terminalGroupMap = bindings.stream()
                .collect(Collectors.toMap(
                        UserGroupTerminalGroupBinding::getTgid,
                        binding -> terminalGroupService.getTerminalGroupById(binding.getTgid()),
                        (existing, replacement) -> existing
                ));
        
        // 按深度排序，同时考虑绑定类型（INCLUDE优先于EXCLUDE处理）
        List<UserGroupTerminalGroupBinding> sortedBindings = bindings.stream()
                .sorted((b1, b2) -> {
                    TerminalGroup tg1 = terminalGroupMap.get(b1.getTgid());
                    TerminalGroup tg2 = terminalGroupMap.get(b2.getTgid());
                    if (tg1 == null || tg2 == null) return 0;
                    
                    // 首先按深度排序
                    int depthCompare = Integer.compare(calculateDepth(tg1.getPath()), calculateDepth(tg2.getPath()));
                    if (depthCompare != 0) return depthCompare;
                    
                    // 同一深度时，INCLUDE优先处理
                    if (b1.getBindingType() != b2.getBindingType()) {
                        return b1.getBindingType() == BindingType.INCLUDE ? -1 : 1;
                    }
                    
                    return 0;
                })
                .toList();
        
        List<UserGroupTerminalGroupBinding> optimizedBindings = new ArrayList<>();
        // 使用复合键跟踪已处理的绑定：tgid + bindingType
        Set<String> processedBindingKeys = new HashSet<>();
        int redundancyRemoved = 0;
        
        for (UserGroupTerminalGroupBinding binding : sortedBindings) {
            String bindingKey = binding.getTgid() + "_" + binding.getBindingType();
            
            if (processedBindingKeys.contains(bindingKey)) {
                redundancyRemoved++;
                continue;
            }
            
            // 检查是否被更高层级的同类型绑定覆盖
            boolean isRedundant = checkRedundancyByParentBinding(binding, optimizedBindings, terminalGroupMap);
            
            if (isRedundant) {
                redundancyRemoved++;
            } else {
                optimizedBindings.add(binding);
                processedBindingKeys.add(bindingKey);
                
                // 标记被此绑定覆盖的子节点绑定（仅相同类型）
                if (binding.getIncludeSub()) {
                    markCoveredChildBindings(binding, processedBindingKeys);
                }
            }
        }
        optimizeWatch.stop();
        log.info("[权限表达式更新] 智能冗余清理 - 耗时: {}",  optimizeWatch.getTotalTimeMillis());
        return new PermissionOptimizationResult(optimizedBindings, redundancyRemoved);
    }
    
    /**
     * 检查绑定是否被父级绑定覆盖（仅检查相同类型的绑定）
     */
    private boolean checkRedundancyByParentBinding(UserGroupTerminalGroupBinding currentBinding,
                                                 List<UserGroupTerminalGroupBinding> existingBindings,
                                                 Map<Long, TerminalGroup> terminalGroupMap) {
        TerminalGroup currentGroup = terminalGroupMap.get(currentBinding.getTgid());
        if (currentGroup == null) return false;
        
        for (UserGroupTerminalGroupBinding existingBinding : existingBindings) {
            // 只有相同绑定类型才可能产生冗余
            if (existingBinding.getBindingType() != currentBinding.getBindingType()) {
                continue;
            }
            
            // 只有includeChildren=true的绑定才能覆盖子节点
            if (!existingBinding.getIncludeSub()) {
                continue;
            }
            
            TerminalGroup existingGroup = terminalGroupMap.get(existingBinding.getTgid());
            if (existingGroup != null && isChildOf(currentGroup, existingGroup)) {
                log.debug("[权限优化] 发现冗余绑定 - 子节点: {}({}), 被父节点覆盖: {}({})", 
                        currentBinding.getTgid(), currentBinding.getBindingType(),
                        existingBinding.getTgid(), existingBinding.getBindingType());
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * 标记被当前绑定覆盖的子节点绑定（仅标记相同类型）
     */
    private void markCoveredChildBindings(UserGroupTerminalGroupBinding parentBinding,
                                        Set<String> processedBindingKeys) {
        if (!parentBinding.getIncludeSub()) {
            return;
        }
        
        List<TerminalGroup> childGroups = terminalGroupService.getChildGroups(parentBinding.getTgid());
        for (TerminalGroup child : childGroups) {
            // 只标记相同类型的绑定为已处理
            String childBindingKey = child.getTgid() + "_" + parentBinding.getBindingType();
            processedBindingKeys.add(childBindingKey);
            
            log.debug("[权限优化] 标记子节点绑定为已覆盖 - 子节点: {}({}), 父节点: {}({})", 
                    child.getTgid(), parentBinding.getBindingType(),
                    parentBinding.getTgid(), parentBinding.getBindingType());
        }
    }

    /**
     * 构建操作统计信息
     */
    private PermissionExpressionResultDTO.OperationStatisticsDTO buildOperationStatistics(
            int originalCount, int optimizedCount, int redundancyRemoved,
            List<UserGroupTerminalGroupBinding> oldBindings, 
            List<UserGroupTerminalGroupBinding> newBindings) {
        
        Set<Long> oldTgids = oldBindings.stream()
                .map(UserGroupTerminalGroupBinding::getTgid)
                .collect(Collectors.toSet());
        
        Set<Long> newTgids = newBindings.stream()
                .map(UserGroupTerminalGroupBinding::getTgid)
                .collect(Collectors.toSet());
        
        int addedCount = (int) newTgids.stream().filter(tgid -> !oldTgids.contains(tgid)).count();
        int deletedCount = (int) oldTgids.stream().filter(tgid -> !newTgids.contains(tgid)).count();
        int updatedCount = (int) newTgids.stream().filter(oldTgids::contains).count();
        
        double optimizationRatio = originalCount > 0 ? 
                (double) redundancyRemoved / originalCount * 100 : 0.0;
        
        return PermissionExpressionResultDTO.OperationStatisticsDTO.builder()
                .originalCount(originalCount)
                .optimizedCount(optimizedCount)
                .redundancyRemoved(redundancyRemoved)
                .addedCount(addedCount)
                .updatedCount(updatedCount)
                .deletedCount(deletedCount)
                .optimizationRatio(optimizationRatio)
                .build();
    }

    /**
     * 转换为优化后的绑定DTO列表
     */
    private List<PermissionExpressionResultDTO.OptimizedBindingDTO> convertToOptimizedBindings(
            List<UserGroupTerminalGroupBinding> bindings) {
        
        return bindings.stream()
                .map(binding -> {
                    TerminalGroup terminalGroup = terminalGroupService.getTerminalGroupById(binding.getTgid());
                    
                    return PermissionExpressionResultDTO.OptimizedBindingDTO.builder()
                            .tgid(binding.getTgid())
                            .terminalGroupName(terminalGroup != null ? terminalGroup.getName() : "未知终端组")
                            .bindingType(binding.getBindingType())
                            .includeChildren(binding.getIncludeSub())
                            .depth(terminalGroup != null ? calculateDepth(terminalGroup.getPath()) : 0)
                            .parentTgid(terminalGroup != null ? terminalGroup.getParent() : null)
                            .optimized(true)
                            .build();
                })
                .collect(Collectors.toList());
    }

    /**
     * 构建操作详情列表
     */
    private List<PermissionExpressionResultDTO.OperationDetailDTO> buildOperationDetails(
            List<UserGroupTerminalGroupBinding> oldBindings,
            List<UserGroupTerminalGroupBinding> newBindings) {
        
        List<PermissionExpressionResultDTO.OperationDetailDTO> details = new ArrayList<>();
        
        Map<Long, UserGroupTerminalGroupBinding> oldBindingMap = oldBindings.stream()
                .collect(Collectors.toMap(UserGroupTerminalGroupBinding::getTgid, b -> b));
        
        Map<Long, UserGroupTerminalGroupBinding> newBindingMap = newBindings.stream()
                .collect(Collectors.toMap(UserGroupTerminalGroupBinding::getTgid, b -> b));
        
        // 处理新增的绑定
        for (UserGroupTerminalGroupBinding newBinding : newBindings) {
            if (!oldBindingMap.containsKey(newBinding.getTgid())) {
                TerminalGroup terminalGroup = terminalGroupService.getTerminalGroupById(newBinding.getTgid());
                details.add(PermissionExpressionResultDTO.OperationDetailDTO.builder()
                        .tgid(newBinding.getTgid())
                        .terminalGroupName(terminalGroup != null ? terminalGroup.getName() : "未知终端组")
                        .operationType("CREATE")
                        .newBinding(formatBinding(newBinding))
                        .reason("新增权限绑定")
                        .success(true)
                        .build());
            }
        }
        
        // 处理删除的绑定
        for (UserGroupTerminalGroupBinding oldBinding : oldBindings) {
            if (!newBindingMap.containsKey(oldBinding.getTgid())) {
                TerminalGroup terminalGroup = terminalGroupService.getTerminalGroupById(oldBinding.getTgid());
                details.add(PermissionExpressionResultDTO.OperationDetailDTO.builder()
                        .tgid(oldBinding.getTgid())
                        .terminalGroupName(terminalGroup != null ? terminalGroup.getName() : "未知终端组")
                        .operationType("DELETE")
                        .oldBinding(formatBinding(oldBinding))
                        .reason("删除权限绑定")
                        .success(true)
                        .build());
            }
        }
        
        // 处理更新的绑定
        for (Long tgid : newBindingMap.keySet()) {
            if (oldBindingMap.containsKey(tgid)) {
                UserGroupTerminalGroupBinding oldBinding = oldBindingMap.get(tgid);
                UserGroupTerminalGroupBinding newBinding = newBindingMap.get(tgid);
                
                if (!bindingsEqual(oldBinding, newBinding)) {
                    TerminalGroup terminalGroup = terminalGroupService.getTerminalGroupById(tgid);
                    details.add(PermissionExpressionResultDTO.OperationDetailDTO.builder()
                            .tgid(tgid)
                            .terminalGroupName(terminalGroup != null ? terminalGroup.getName() : "未知终端组")
                            .operationType("UPDATE")
                            .oldBinding(formatBinding(oldBinding))
                            .newBinding(formatBinding(newBinding))
                            .reason("更新权限绑定")
                            .success(true)
                            .build());
                }
            }
        }
        
        return details;
    }

    /**
     * 转换为权限绑定状态DTO列表
     */
    private List<UserGroupPermissionStatusDTO.PermissionBindingStatusDTO> convertToPermissionBindingStatuses(
            List<UserGroupTerminalGroupBinding> bindings) {
        
        return bindings.stream()
                .map(binding -> {
                    TerminalGroup terminalGroup = terminalGroupService.getTerminalGroupById(binding.getTgid());
                    
                    return UserGroupPermissionStatusDTO.PermissionBindingStatusDTO.builder()
                            .bindingId(binding.getBindingId())
                            .tgid(binding.getTgid())
                            .terminalGroupName(terminalGroup != null ? terminalGroup.getName() : "未知终端组")
                            .terminalGroupPath(terminalGroup != null ? terminalGroup.getPath() : "")
                            .bindingType(binding.getBindingType())
                            .includeChildren(binding.getIncludeSub())
                            .depth(terminalGroup != null ? calculateDepth(terminalGroup.getPath()) : 0)
                            .parentTgid(terminalGroup != null ? terminalGroup.getParent() : null)
                            .childCount(0) // TODO: 计算子组数量
                            .effectiveStatus("EFFECTIVE")
                            .createTime(binding.getCreateTime())
                            .updateTime(binding.getUpdateTime())
                            .creator("系统") // TODO: 从用户信息获取
                            .build();
                })
                .collect(Collectors.toList());
    }

    /**
     * 计算绑定统计信息
     */
    private UserGroupPermissionStatusDTO.BindingStatisticsDTO calculateBindingStatistics(
            List<UserGroupTerminalGroupBinding> bindings) {
        
        int totalBindings = bindings.size();
        int includeBindings = (int) bindings.stream()
                .filter(b -> b.getBindingType() == BindingType.INCLUDE)
                .count();
        int excludeBindings = totalBindings - includeBindings;
        int includeChildrenBindings = (int) bindings.stream()
                .filter(UserGroupTerminalGroupBinding::getIncludeSub)
                .count();
        
        // TODO: 实现更复杂的统计逻辑
        // 简化实现
        int maxDepth = bindings.stream()
                .map(UserGroupTerminalGroupBinding::getTgid)
                .map(terminalGroupService::getTerminalGroupById)
                .filter(Objects::nonNull)
                .mapToInt(tg -> calculateDepth(tg.getPath()))
                .max()
                .orElse(0);
        
        return UserGroupPermissionStatusDTO.BindingStatisticsDTO.builder()
                .totalBindings(totalBindings)
                .includeBindings(includeBindings)
                .excludeBindings(excludeBindings)
                .includeChildrenBindings(includeChildrenBindings)
                .totalCoveredTerminalGroups(totalBindings)
                .maxDepth(maxDepth)
                .coveragePercentage(85.5) // TODO: 实现真实的覆盖率计算
                .build();
    }

    /**
     * 格式化绑定信息为字符串
     */
    private String formatBinding(UserGroupTerminalGroupBinding binding) {
        return String.format("%s(%s)", 
                binding.getBindingType(), 
                binding.getIncludeSub() ? "包含子组" : "仅当前组");
    }

    /**
     * 比较两个绑定是否相等
     */
    private boolean bindingsEqual(UserGroupTerminalGroupBinding binding1, UserGroupTerminalGroupBinding binding2) {
        return Objects.equals(binding1.getBindingType(), binding2.getBindingType()) &&
               Objects.equals(binding1.getIncludeSub(), binding2.getIncludeSub());
    }

    /**
     * 计算终端组的层级深度
     */
    private Integer calculateDepth(String path) {
        if (path == null || path.isEmpty()) {
            return 0;
        }
        // 路径格式如："|1|2|3|"，计算深度就是分隔符的数量减1
        long pipeCount = path.chars().filter(ch -> ch == '|').count();
        return (int) Math.max(0, pipeCount - 1);
    }

    /**
     * 记录详细的操作变更日志
     */
    private void logOperationDetails(Long ugid, List<UserGroupTerminalGroupBinding> oldBindings,
                                   List<UserGroupTerminalGroupBinding> newBindings) {
        try {
            Map<Long, UserGroupTerminalGroupBinding> oldBindingMap = oldBindings.stream()
                    .collect(Collectors.toMap(UserGroupTerminalGroupBinding::getTgid, b -> b, (e1, e2) -> e1));
            
            Map<Long, UserGroupTerminalGroupBinding> newBindingMap = newBindings.stream()
                    .collect(Collectors.toMap(UserGroupTerminalGroupBinding::getTgid, b -> b, (e1, e2) -> e1));
            
            // 记录新增的绑定
            for (UserGroupTerminalGroupBinding newBinding : newBindings) {
                if (!oldBindingMap.containsKey(newBinding.getTgid())) {
                    log.info("[权限变更详情] 新增权限绑定 - 用户组ID: {}, 终端组ID: {}, 绑定类型: {}, 包含子组: {}", 
                            ugid, newBinding.getTgid(), newBinding.getBindingType(), newBinding.getIncludeSub());
                }
            }
            
            // 记录删除的绑定
            for (UserGroupTerminalGroupBinding oldBinding : oldBindings) {
                if (!newBindingMap.containsKey(oldBinding.getTgid())) {
                    log.info("[权限变更详情] 删除权限绑定 - 用户组ID: {}, 终端组ID: {}, 原绑定类型: {}, 原包含子组: {}", 
                            ugid, oldBinding.getTgid(), oldBinding.getBindingType(), oldBinding.getIncludeSub());
                }
            }
            
            // 记录更新的绑定
            for (Long tgid : newBindingMap.keySet()) {
                if (oldBindingMap.containsKey(tgid)) {
                    UserGroupTerminalGroupBinding oldBinding = oldBindingMap.get(tgid);
                    UserGroupTerminalGroupBinding newBinding = newBindingMap.get(tgid);
                    
                    if (!bindingsEqual(oldBinding, newBinding)) {
                        log.info("[权限变更详情] 更新权限绑定 - 用户组ID: {}, 终端组ID: {}, 原设置: {}({}), 新设置: {}({})", 
                                ugid, tgid,
                                oldBinding.getBindingType(), oldBinding.getIncludeSub() ? "包含子组" : "仅当前组",
                                newBinding.getBindingType(), newBinding.getIncludeSub() ? "包含子组" : "仅当前组");
                    }
                }
            }
            
            // 记录绑定类型统计
            int newIncludeCount = (int) newBindings.stream().filter(b -> b.getBindingType() == BindingType.INCLUDE).count();
            int newExcludeCount = newBindings.size() - newIncludeCount;
            int oldIncludeCount = (int) oldBindings.stream().filter(b -> b.getBindingType() == BindingType.INCLUDE).count();
            int oldExcludeCount = oldBindings.size() - oldIncludeCount;
            
            log.info("[权限统计变更] 用户组ID: {}, 绑定类型变化 - INCLUDE: {} -> {}, EXCLUDE: {} -> {}", 
                    ugid, oldIncludeCount, newIncludeCount, oldExcludeCount, newExcludeCount);
            
        } catch (Exception e) {
            log.warn("[权限变更详情] 记录操作详情时发生异常 - 用户组ID: {}, 错误: {}", ugid, e.getMessage());
        }
    }

    /**
     * 权限优化结果
     */
    private static class PermissionOptimizationResult {
        private final List<UserGroupTerminalGroupBinding> optimizedBindings;
        private final int redundancyRemoved;
        
        public PermissionOptimizationResult(List<UserGroupTerminalGroupBinding> optimizedBindings, int redundancyRemoved) {
            this.optimizedBindings = optimizedBindings;
            this.redundancyRemoved = redundancyRemoved;
        }
        
        public List<UserGroupTerminalGroupBinding> getOptimizedBindings() {
            return optimizedBindings;
        }
        
        public int getRedundancyRemoved() {
            return redundancyRemoved;
        }
    }
}