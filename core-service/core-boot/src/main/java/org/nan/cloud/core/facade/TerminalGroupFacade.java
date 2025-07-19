package org.nan.cloud.core.facade;

import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.nan.cloud.common.basic.exception.ExceptionEnum;
import org.nan.cloud.common.basic.model.BindingType;
import org.nan.cloud.common.basic.model.PageRequestDTO;
import org.nan.cloud.common.basic.model.PageVO;
import org.nan.cloud.common.web.context.InvocationContextHolder;
import org.nan.cloud.common.web.context.RequestUserInfo;
import org.nan.cloud.core.DTO.*;
import org.nan.cloud.core.api.DTO.common.TerminalGroupTreeNode;
import org.nan.cloud.core.api.DTO.req.*;
import org.nan.cloud.core.api.DTO.res.*;
import org.nan.cloud.core.aspect.SkipOrgManagerPermissionCheck;
import org.nan.cloud.core.converter.TerminalGroupConverter;
import org.nan.cloud.core.converter.UserTerminalGroupBindingConverter;
import org.nan.cloud.core.domain.Organization;
import org.nan.cloud.core.domain.TerminalGroup;
import org.nan.cloud.core.service.*;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class TerminalGroupFacade {

    private final TerminalGroupService terminalGroupService;
    private final UserGroupTerminalGroupBindingService bindingService;
    private final OrgService orgService;
    private final PermissionChecker permissionChecker;
    private final TerminalGroupConverter terminalGroupConverter;
    private final UserTerminalGroupBindingConverter  userTerminalGroupBindingConverter;

    public TerminalGroupTreeResponse getTerminalGroupTree() {
        RequestUserInfo userInfo = InvocationContextHolder.getContext().getRequestUser();
        Long oid = userInfo.getOid();
        
        // 获取组织信息
        Organization organization = orgService.getOrgByOid(oid);
        
        // 获取用户可访问的终端组ID列表（通过Service层计算INCLUDE/EXCLUDE权限）
        List<Long> accessibleTerminalGroupIds = bindingService.getAccessibleTerminalGroupIds(userInfo.getUgid());
        
        // 构建终端组树列表
        List<TerminalGroupTreeNode> accessibleTrees = buildAccessibleTerminalGroupTrees(accessibleTerminalGroupIds, oid);
        
        TerminalGroupTreeResponse response = new TerminalGroupTreeResponse();
        response.setOrganization(terminalGroupConverter.organization2OrganizationDTO(organization));
        response.setAccessibleTrees(accessibleTrees);
        
        return response;
    }

    @Transactional
    @SkipOrgManagerPermissionCheck
    public void createTerminalGroup(CreateTerminalGroupRequest request) {
        RequestUserInfo userInfo = InvocationContextHolder.getContext().getRequestUser();


        ExceptionEnum.ORG_PERMISSION_DENIED.throwIf(!permissionChecker.ifTargetTerminalGroupTheSameOrg(userInfo.getOid(), request.getParentTgid()));
        ExceptionEnum.TERMINAL_GROUP_PERMISSION_DENIED.throwIf(!permissionChecker.ifHasPermissionOnTargetTerminalGroup(userInfo.getUgid(), request.getParentTgid()));
        
        CreateTerminalGroupDTO createDTO = CreateTerminalGroupDTO.builder()
                .terminalGroupName(request.getTerminalGroupName())
                .parentTgid(request.getParentTgid())
                .description(request.getDescription())
                .oid(userInfo.getOid())
                .creatorId(userInfo.getUid())
                .build();
        
        terminalGroupService.createTerminalGroup(createDTO);
    }

    @Transactional
    @SkipOrgManagerPermissionCheck
    public void deleteTerminalGroup(Long tgid) {
        RequestUserInfo userInfo = InvocationContextHolder.getContext().getRequestUser();
        ExceptionEnum.ORG_PERMISSION_DENIED.throwIf(permissionChecker.ifTargetTerminalGroupTheSameOrg(userInfo.getOid(), tgid));
        ExceptionEnum.TERMINAL_GROUP_PERMISSION_DENIED.throwIf(permissionChecker.ifHasPermissionOnTargetTerminalGroup(userInfo.getUgid(), tgid));
        terminalGroupService.deleteTerminalGroup(tgid, userInfo.getUid());
    }

    @Transactional
    @SkipOrgManagerPermissionCheck
    public void updateTerminalGroup(UpdateTerminalGroupRequest request) {
        RequestUserInfo userInfo = InvocationContextHolder.getContext().getRequestUser();
        ExceptionEnum.ORG_PERMISSION_DENIED.throwIf(permissionChecker.ifTargetTerminalGroupTheSameOrg(userInfo.getOid(), request.getTgid()));
        ExceptionEnum.TERMINAL_GROUP_PERMISSION_DENIED.throwIf(permissionChecker.ifHasPermissionOnTargetTerminalGroup(userInfo.getUgid(), request.getTgid()));

        UpdateTerminalGroupDTO updateDTO = UpdateTerminalGroupDTO.builder()
                .tgid(request.getTgid())
                .terminalGroupName(request.getTerminalGroupName())
                .description(request.getDescription())
                .updaterId(userInfo.getUid())
                .build();
        
        terminalGroupService.updateTerminalGroup(updateDTO);
    }

    public TerminalGroupDetailResponse getTerminalGroupDetail(Long tgid) {
        RequestUserInfo userInfo = InvocationContextHolder.getContext().getRequestUser();
        ExceptionEnum.ORG_PERMISSION_DENIED.throwIf(permissionChecker.ifTargetTerminalGroupTheSameOrg(userInfo.getOid(), tgid));
        ExceptionEnum.TERMINAL_GROUP_PERMISSION_DENIED.throwIf(permissionChecker.ifHasPermissionOnTargetTerminalGroup(userInfo.getUgid(), tgid));
        TerminalGroup terminalGroup = terminalGroupService.getTerminalGroupById(tgid);
        return terminalGroupConverter.terminalGroup2DetailResponse(terminalGroup);
    }


    /**
     * 构建用户可访问的终端组树列表
     * 处理多对多关系和去重逻辑
     */
    private List<TerminalGroupTreeNode> buildAccessibleTerminalGroupTrees(List<Long> accessibleTerminalGroupIds, Long oid) {
        if (CollectionUtils.isEmpty(accessibleTerminalGroupIds)) {
            return Collections.emptyList();
        }
        
        // 获取所有相关的终端组（包括这些终端组的父组，用于构建完整路径）
        Set<Long> allRelatedTerminalGroupIds = new HashSet<>(accessibleTerminalGroupIds);
        Map<Long, TerminalGroup> terminalGroupMap = new HashMap<>();
        
        // 获取所有终端组详情
        for (Long tgid : accessibleTerminalGroupIds) {
            TerminalGroup terminalGroup = terminalGroupService.getTerminalGroupById(tgid);
            if (terminalGroup != null) {
                terminalGroupMap.put(tgid, terminalGroup);
                // 添加父组路径中的所有组ID
                addParentGroupIds(terminalGroup.getPath(), allRelatedTerminalGroupIds);
            }
        }
        
        // 获取所有相关终端组的详情
        for (Long tgid : allRelatedTerminalGroupIds) {
            if (!terminalGroupMap.containsKey(tgid)) {
                TerminalGroup terminalGroup = terminalGroupService.getTerminalGroupById(tgid);
                if (terminalGroup != null) {
                    terminalGroupMap.put(tgid, terminalGroup);
                }
            }
        }
        
        // 构建树节点映射
        Map<Long, TerminalGroupTreeNode> nodeMap = new HashMap<>();
        for (TerminalGroup terminalGroup : terminalGroupMap.values()) {
            TerminalGroupTreeNode node = terminalGroupConverter.terminalGroup2TreeNode(terminalGroup);
            node.setChildren(new ArrayList<>());
            // 只有用户真正有权限访问的组才设置为有权限
            node.setHasPermission(accessibleTerminalGroupIds.contains(terminalGroup.getTgid()));
            nodeMap.put(terminalGroup.getTgid(), node);
        }
        
        // 构建父子关系
        Set<Long> rootNodeIds = new HashSet<>();
        for (TerminalGroupTreeNode node : nodeMap.values()) {
            if (node.getParent() != null && nodeMap.containsKey(node.getParent())) {
                nodeMap.get(node.getParent()).getChildren().add(node);
            } else {
                // 没有父组或父组不在可访问范围内的作为根节点
                rootNodeIds.add(node.getTgid());
            }
        }
        
        // 返回所有根节点，形成森林结构
        return rootNodeIds.stream()
                .map(nodeMap::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
    
    /**
     * 从路径中提取所有父组ID
     */
    private void addParentGroupIds(String path, Set<Long> groupIds) {
        if (path == null || path.isEmpty()) {
            return;
        }
        
        String[] pathParts = path.split("\\|");
        for (String part : pathParts) {
            if (!part.trim().isEmpty()) {
                try {
                    groupIds.add(Long.valueOf(part.trim()));
                } catch (NumberFormatException e) {
                    // 忽略无效的路径部分
                }
            }
        }
    }

    @Transactional
    @SkipOrgManagerPermissionCheck
    public PermissionExpressionResponse updatePermissionExpression(PermissionExpressionRequest request) {
        RequestUserInfo userInfo = InvocationContextHolder.getContext().getRequestUser();
        ExceptionEnum.ORG_PERMISSION_DENIED.throwIf(!permissionChecker.ifTargetUserGroupIsTheSameOrg(userInfo.getUgid(), request.getUgid()));
        // 权限检查：确保操作用户有权限修改目标用户组的绑定
        List<Long> targetTgids = request.getPermissionBindings().stream().map(PermissionExpressionRequest.PermissionBinding::getTgid).toList();
        permissionChecker.canModifyUserGroupTerminalGroupBinding(userInfo.getOid(), userInfo.getUgid(), request.getUgid(), targetTgids);
        
        // 转换为内部DTO
        PermissionExpressionDTO internalRequest = userTerminalGroupBindingConverter.convertToPermissionExpressionDTO(request, userInfo);
        
        // 执行权限表达式更新
        PermissionExpressionResultDTO result = bindingService.updatePermissionExpression(internalRequest);
        
        // 转换为响应对象
        return userTerminalGroupBindingConverter.convertToPermissionExpressionResponse(result);
    }


    public UserGroupPermissionStatusResponse getUserGroupPermissionStatus(Long targetUgid) {
        RequestUserInfo userInfo = InvocationContextHolder.getContext().getRequestUser();
        boolean ifSelfUgid = userInfo.getUgid().equals(targetUgid);
        boolean isOrgManager = InvocationContextHolder.ifOrgManager();
        
        // 权限检查：确保操作用户有权限查看目标用户组的权限状态
        if (!ifSelfUgid) {
            permissionChecker.canViewUserGroupTerminalGroupBinding(userInfo.getOid(), userInfo.getUgid(), targetUgid);
        }
        
        // 高效策略：先计算可见权限范围，再获取和构造响应
        Set<Long> visibleTgids = null;
        if (!ifSelfUgid && !isOrgManager) {
            // 非本人且非组织管理员：需要计算可见权限范围
            log.debug("[权限继承] 开始计算可见权限范围 - 操作者: {}, 目标: {}", userInfo.getUgid(), targetUgid);
            
            UserGroupPermissionStatusDTO operatorUgStatus = bindingService.getUserGroupPermissionStatus(userInfo.getUgid());
            visibleTgids = getVisibleTerminalGroupIds(operatorUgStatus);
            
            log.debug("[权限继承] 操作者可见权限 - 操作者: {}, 可见数量: {}", userInfo.getUgid(), visibleTgids.size());
            
            if (visibleTgids.isEmpty()) {
                // 操作者无任何可见权限，直接返回空结果
                log.warn("[权限继承] 操作者无可见权限 - 操作者: {}, 目标: {}", userInfo.getUgid(), targetUgid);
                return createEmptyPermissionStatusResponse(targetUgid);
            }
        }
        
        // 获取目标用户组的权限状态
        UserGroupPermissionStatusDTO targetUgStatus = bindingService.getUserGroupPermissionStatus(targetUgid);
        
        // 高效过滤：在DTO转换前先过滤数据
        if (visibleTgids != null) {
            targetUgStatus = filterPermissionStatusByVisibleScope(targetUgStatus, visibleTgids, userInfo.getUgid(), targetUgid);
        }
        
        // 转换为响应对象（只需转换一次）
        return userTerminalGroupBindingConverter.convertToUserGroupPermissionStatusResponse(targetUgStatus);
    }
    
    /**
     * 高效权限过滤：按可见权限范围过滤目标用户组的权限状态
     * 优化策略：先过滤数据，再进行统计计算，减少不必要的计算开销
     * 
     * @param targetUgStatus 目标用户组的权限状态
     * @param visibleTgids 操作者可见的终端组ID集合
     * @param operatorUgid 操作者用户组ID
     * @param targetUgid 目标用户组ID
     * @return 过滤后的权限状态
     */
    private UserGroupPermissionStatusDTO filterPermissionStatusByVisibleScope(
            UserGroupPermissionStatusDTO targetUgStatus, 
            Set<Long> visibleTgids,
            Long operatorUgid,
            Long targetUgid) {
        
        try {
            List<UserGroupPermissionStatusDTO.PermissionBindingStatusDTO> originalBindings = 
                    targetUgStatus.getPermissionBindings() != null ? targetUgStatus.getPermissionBindings() : Collections.emptyList();
            
            // 高效过滤：只保留可见的终端组绑定
            List<UserGroupPermissionStatusDTO.PermissionBindingStatusDTO> filteredBindings = 
                    originalBindings.stream()
                    .filter(binding -> visibleTgids.contains(binding.getTgid()))
                    .collect(Collectors.toList());
            
            log.debug("[权限过滤] 绑定过滤结果 - 操作者: {}, 目标: {}, 原始: {}, 过滤后: {}", 
                    operatorUgid, targetUgid, originalBindings.size(), filteredBindings.size());
            
            // 一次性计算过滤后的统计信息
            UserGroupPermissionStatusDTO.BindingStatisticsDTO filteredStatistics = 
                    calculateStatisticsEfficiently(filteredBindings);
            
            // 构建过滤后的权限状态
            return UserGroupPermissionStatusDTO.builder()
                    .ugid(targetUgStatus.getUgid())
                    .userGroupName(targetUgStatus.getUserGroupName())
                    .permissionBindings(filteredBindings)
                    .statistics(filteredStatistics)
                    .lastUpdateTime(targetUgStatus.getLastUpdateTime())
                    .build();
            
        } catch (Exception e) {
            log.error("[权限过滤] 权限过滤失败 - 操作者: {}, 目标: {}, 错误: {}", 
                    operatorUgid, targetUgid, e.getMessage(), e);
            // 异常情况下返回空结果，保证安全
            return createEmptyPermissionStatus(targetUgStatus.getUgid());
        }
    }
    
    /**
     * 获取用户组的可见终端组ID集合
     * 基于INCLUDE/EXCLUDE绑定类型计算最终的可见权限
     */
    private Set<Long> getVisibleTerminalGroupIds(UserGroupPermissionStatusDTO ugStatus) {
        if (ugStatus.getPermissionBindings() == null) {
            return Collections.emptySet();
        }
        
        Set<Long> includedTgids = new HashSet<>();
        Set<Long> excludedTgids = new HashSet<>();
        
        // 处理INCLUDE绑定
        for (UserGroupPermissionStatusDTO.PermissionBindingStatusDTO binding : ugStatus.getPermissionBindings()) {
            if (binding.getBindingType() == BindingType.INCLUDE) {
                includedTgids.add(binding.getTgid());
                
                // 处理包含子组的情况
                if (binding.getIncludeChildren()) {
                    Set<Long> childTgids = getChildTerminalGroupIds(binding.getTgid());
                    includedTgids.addAll(childTgids);
                }
            }
        }
        
        // 处理EXCLUDE绑定
        for (UserGroupPermissionStatusDTO.PermissionBindingStatusDTO binding : ugStatus.getPermissionBindings()) {
            if (binding.getBindingType() == BindingType.EXCLUDE) {
                excludedTgids.add(binding.getTgid());
                
                // 处理包含子组的情况
                if (binding.getIncludeChildren()) {
                    Set<Long> childTgids = getChildTerminalGroupIds(binding.getTgid());
                    excludedTgids.addAll(childTgids);
                }
            }
        }
        
        // 计算最终可见权限：INCLUDE - EXCLUDE
        includedTgids.removeAll(excludedTgids);
        
        return includedTgids;
    }
    
    /**
     * 获取终端组的所有子组ID
     */
    private Set<Long> getChildTerminalGroupIds(Long parentTgid) {
        try {
            List<TerminalGroup> childGroups = terminalGroupService.getChildGroups(parentTgid);
            return childGroups.stream()
                    .map(TerminalGroup::getTgid)
                    .collect(Collectors.toSet());
        } catch (Exception e) {
            log.warn("[权限过滤] 获取子终端组失败 - 父组ID: {}, 错误: {}", parentTgid, e.getMessage());
            return Collections.emptySet();
        }
    }
    
    /**
     * 重新计算过滤后的统计信息
     */
    private UserGroupPermissionStatusDTO.BindingStatisticsDTO recalculateStatistics(
            List<UserGroupPermissionStatusDTO.PermissionBindingStatusDTO> filteredBindings) {
        
        int totalBindings = filteredBindings.size();
        int includeBindings = (int) filteredBindings.stream()
                .filter(b -> b.getBindingType() == BindingType.INCLUDE)
                .count();
        int excludeBindings = totalBindings - includeBindings;
        int includeChildrenBindings = (int) filteredBindings.stream()
                .filter(UserGroupPermissionStatusDTO.PermissionBindingStatusDTO::getIncludeChildren)
                .count();
        
        int maxDepth = filteredBindings.stream()
                .mapToInt(UserGroupPermissionStatusDTO.PermissionBindingStatusDTO::getDepth)
                .max()
                .orElse(0);
        
        // 计算实际覆盖的终端组数量（简化版）
        int totalCoveredTerminalGroups = totalBindings;
        
        return UserGroupPermissionStatusDTO.BindingStatisticsDTO.builder()
                .totalBindings(totalBindings)
                .includeBindings(includeBindings)
                .excludeBindings(excludeBindings)
                .includeChildrenBindings(includeChildrenBindings)
                .totalCoveredTerminalGroups(totalCoveredTerminalGroups)
                .maxDepth(maxDepth)
                .build();
    }
    
    /**
     * 创建空的权限状态（当操作者无任何可见权限时）
     */
    private UserGroupPermissionStatusDTO createEmptyPermissionStatus(Long ugid) {
        UserGroupPermissionStatusDTO.BindingStatisticsDTO emptyStatistics = 
                UserGroupPermissionStatusDTO.BindingStatisticsDTO.builder()
                .totalBindings(0)
                .includeBindings(0)
                .excludeBindings(0)
                .includeChildrenBindings(0)
                .totalCoveredTerminalGroups(0)
                .maxDepth(0)
                .build();
        
        return UserGroupPermissionStatusDTO.builder()
                .ugid(ugid)
                .userGroupName("用户组")
                .permissionBindings(Collections.emptyList())
                .statistics(emptyStatistics)
                .lastUpdateTime(null)
                .build();
    }
    
    /**
     * 高效计算统计信息（一次遍历完成所有计算）
     */
    private UserGroupPermissionStatusDTO.BindingStatisticsDTO calculateStatisticsEfficiently(
            List<UserGroupPermissionStatusDTO.PermissionBindingStatusDTO> filteredBindings) {
        
        if (filteredBindings.isEmpty()) {
            return createEmptyStatistics();
        }
        
        // 一次遍历计算所有统计数据
        int totalBindings = filteredBindings.size();
        int includeBindings = 0;
        int includeChildrenBindings = 0;
        int maxDepth = 0;
        
        for (UserGroupPermissionStatusDTO.PermissionBindingStatusDTO binding : filteredBindings) {
            // 统计INCLUDE绑定
            if (binding.getBindingType() == BindingType.INCLUDE) {
                includeBindings++;
            }
            
            // 统计包含子组的绑定
            if (binding.getIncludeChildren() != null && binding.getIncludeChildren()) {
                includeChildrenBindings++;
            }
            
            // 计算最大深度
            if (binding.getDepth() != null && binding.getDepth() > maxDepth) {
                maxDepth = binding.getDepth();
            }
        }
        
        int excludeBindings = totalBindings - includeBindings;
        
        return UserGroupPermissionStatusDTO.BindingStatisticsDTO.builder()
                .totalBindings(totalBindings)
                .includeBindings(includeBindings)
                .excludeBindings(excludeBindings)
                .includeChildrenBindings(includeChildrenBindings)
                .totalCoveredTerminalGroups(totalBindings) // 简化计算
                .maxDepth(maxDepth)
                .build();
    }
    
    /**
     * 创建空的权限状态响应（直接返回Response对象，避免DTO转换）
     */
    private UserGroupPermissionStatusResponse createEmptyPermissionStatusResponse(Long ugid) {
        UserGroupPermissionStatusResponse response = new UserGroupPermissionStatusResponse();
        response.setUgid(ugid);
        response.setUserGroupName("用户组");
        response.setPermissionBindings(Collections.emptyList());
        response.setLastUpdateTime(null);
        
        // 直接创建空统计信息
        UserGroupPermissionStatusResponse.BindingStatistics emptyStats = new UserGroupPermissionStatusResponse.BindingStatistics();
        emptyStats.setTotalBindings(0);
        emptyStats.setIncludeBindings(0);
        emptyStats.setExcludeBindings(0);
        emptyStats.setIncludeChildrenBindings(0);
        emptyStats.setTotalCoveredTerminalGroups(0);
        emptyStats.setMaxDepth(0);
        response.setStatistics(emptyStats);
        
        return response;
    }
    
    /**
     * 创建空的统计信息
     */
    private UserGroupPermissionStatusDTO.BindingStatisticsDTO createEmptyStatistics() {
        return UserGroupPermissionStatusDTO.BindingStatisticsDTO.builder()
                .totalBindings(0)
                .includeBindings(0)
                .excludeBindings(0)
                .includeChildrenBindings(0)
                .totalCoveredTerminalGroups(0)
                .maxDepth(0)
                .build();
    }

}