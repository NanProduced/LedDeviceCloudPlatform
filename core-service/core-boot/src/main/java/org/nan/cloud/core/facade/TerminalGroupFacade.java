package org.nan.cloud.core.facade;

import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.nan.cloud.common.basic.exception.ExceptionEnum;
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
import org.nan.cloud.core.domain.Organization;
import org.nan.cloud.core.domain.TerminalGroup;
import org.nan.cloud.core.exception.BusinessException;
import org.nan.cloud.core.service.*;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class TerminalGroupFacade {

    private final TerminalGroupService terminalGroupService;
    private final UserGroupTerminalGroupBindingService bindingService;
    private final OrgService orgService;
    private final PermissionChecker permissionChecker;
    private final TerminalGroupConverter terminalGroupConverter;

    public TerminalGroupTreeResponse getTerminalGroupTree() {
        RequestUserInfo userInfo = InvocationContextHolder.getContext().getRequestUser();
        Long oid = userInfo.getOid();
        
        // 获取组织信息
        Organization organization = orgService.getOrgByOid(oid);
        
        // 获取用户可访问的终端组ID列表（已去重）
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

    public PageVO<TerminalGroupListResponse> searchTerminalGroup(PageRequestDTO<SearchTerminalGroupRequest> requestDTO) {
        RequestUserInfo userInfo = InvocationContextHolder.getContext().getRequestUser();
        // 根据终端组名关键词搜索当前用户有权限的终端组
        SearchTerminalGroupDTO searchDTO = SearchTerminalGroupDTO.builder()
                .keyword(requestDTO.getParams().getKeyword())
                .oid(userInfo.getOid())
                .ugid(userInfo.getUgid())
                .build();
        
        PageVO<TerminalGroup> terminalGroupPage = terminalGroupService.searchAccessibleTerminalGroups(requestDTO.getPageNum(), requestDTO.getPageSize(), searchDTO);
        if (CollectionUtils.isEmpty(terminalGroupPage.getRecords())) {
            return PageVO.empty();
        }
        return terminalGroupPage.map(e -> TerminalGroupListResponse.builder()
                .tgid(e.getTgid())
                .terminalGroupName(e.getName())
                .parent(e.getParent())
                .description(e.getDescription())
                .createTime(e.getCreateTime())
                .build());
    }

    public TerminalGroupDetailResponse getTerminalGroupDetail(Long tgid) {
        RequestUserInfo userInfo = InvocationContextHolder.getContext().getRequestUser();
        ExceptionEnum.ORG_PERMISSION_DENIED.throwIf(permissionChecker.ifTargetTerminalGroupTheSameOrg(userInfo.getOid(), tgid));
        ExceptionEnum.TERMINAL_GROUP_PERMISSION_DENIED.throwIf(permissionChecker.ifHasPermissionOnTargetTerminalGroup(userInfo.getUgid(), tgid));
        TerminalGroup terminalGroup = terminalGroupService.getTerminalGroupById(tgid);
        return terminalGroupConverter.terminalGroup2DetailResponse(terminalGroup);
    }

    
    @Transactional
    @SkipOrgManagerPermissionCheck
    public BatchBindingOperationResponse updateUserGroupPermissions(BatchBindingOperationRequest request) {
        RequestUserInfo userInfo = InvocationContextHolder.getContext().getRequestUser();
        
        // 权限检查：确保操作用户有权限修改目标用户组的绑定
        permissionChecker.canModifyUserGroupTerminalGroupBinding(
                userInfo.getUid(), userInfo.getUgid(), request.getUgid(), null);
        
        // 转换为内部DTO
        BatchBindingOperationDTO internalRequest = BatchBindingOperationDTO.builder()
                .ugid(request.getUgid())
                .bindTgids(request.getGrantTerminalGroupIds())
                .unbindTgids(request.getRevokeTerminalGroupIds())
                .operatorId(userInfo.getUid())
                .operationDescription(request.getDescription())
                .build();
        
        BatchBindingOperationResultDTO result = bindingService.executeBatchBindingOperation(internalRequest);
        
        return convertToBatchBindingOperationResponse(result);
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
    
    /**
     * 转换批量绑定操作结果为响应对象
     */
    private BatchBindingOperationResponse convertToBatchBindingOperationResponse(BatchBindingOperationResultDTO result) {
        BatchBindingOperationResponse response = new BatchBindingOperationResponse();
        response.setSuccess(result.getSuccess());
        response.setMessage(result.getMessage());
        
        // 构建统计信息
        BatchBindingOperationResponse.OperationStatistics statistics = new BatchBindingOperationResponse.OperationStatistics();
        statistics.setGrantedCount(result.getCreatedBindings());
        statistics.setRevokedCount(result.getDeletedBindings());
        
        if (result.getOperationDetails() != null) {
            int noChangeCount = (int) result.getOperationDetails().stream()
                    .filter(detail -> "NO_CHANGE".equals(detail.getOperationType()))
                    .count();
            statistics.setNoChangeCount(noChangeCount);
            
            // 转换操作详情
            List<BatchBindingOperationResponse.OperationDetail> details = result.getOperationDetails().stream()
                    .map(detail -> {
                        BatchBindingOperationResponse.OperationDetail responseDetail = new BatchBindingOperationResponse.OperationDetail();
                        responseDetail.setTgid(detail.getTgid());
                        responseDetail.setTerminalGroupName(detail.getTerminalGroupName());
                        responseDetail.setResult(detail.getOperationType());
                        responseDetail.setDescription(detail.getDescription());
                        return responseDetail;
                    })
                    .collect(Collectors.toList());
            response.setDetails(details);
        }
        
        response.setStatistics(statistics);
        return response;
    }
}