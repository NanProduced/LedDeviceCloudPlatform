package org.nan.cloud.core.facade;

import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.nan.cloud.common.basic.exception.ExceptionEnum;
import org.nan.cloud.common.basic.model.PageRequestDTO;
import org.nan.cloud.common.basic.model.PageVO;
import org.nan.cloud.common.web.context.InvocationContextHolder;
import org.nan.cloud.common.web.context.RequestUserInfo;
import org.nan.cloud.core.DTO.*;
import org.nan.cloud.core.api.DTO.common.OrganizationDTO;
import org.nan.cloud.core.api.DTO.common.TerminalGroupTreeNode;
import org.nan.cloud.core.api.DTO.req.*;
import org.nan.cloud.core.api.DTO.res.*;
import org.nan.cloud.core.aspect.SkipOrgManagerPermissionCheck;
import org.nan.cloud.core.converter.TerminalGroupConverter;
import org.nan.cloud.core.domain.Organization;
import org.nan.cloud.core.domain.TerminalGroup;
import org.nan.cloud.core.domain.TerminalDevice;
import org.nan.cloud.core.enums.UserTypeEnum;
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
        RequestUserInfo userInfo = InvocationContextHolder.getInvocationContext().getRequestUserInfo();
        Long oid = userInfo.getOid();
        
        // 获取组织信息
        Organization organization = orgService.getOrgByOid(oid);
        if (organization == null) {
            throw new BusinessException(ExceptionEnum.ORGANIZATION_NOT_FOUND);
        }
        
        // 获取用户可访问的终端组列表
        List<TerminalGroup> accessibleGroups = terminalGroupService.getAccessibleTerminalGroups(userInfo.getUid(), oid);
        
        // 构建树形结构
        TerminalGroupTreeNode rootNode = buildTerminalGroupTree(accessibleGroups, oid);
        
        TerminalGroupTreeResponse response = new TerminalGroupTreeResponse();
        response.setOrganization(terminalGroupConverter.organization2OrganizationDTO(organization));
        response.setRoot(rootNode);
        
        return response;
    }

    @Transactional
    @SkipOrgManagerPermissionCheck
    public void createTerminalGroup(CreateTerminalGroupRequest request) {
        RequestUserInfo userInfo = InvocationContextHolder.getInvocationContext().getRequestUserInfo();
        
        // 权限检查 - 组织管理员跳过权限检查
        if (!userInfo.getUserType().equals(UserTypeEnum.ORG_MANAGER_USER.getCode())) {
            if (!permissionChecker.hasTerminalGroupManagePermission(userInfo.getUid(), request.getParentTgid())) {
                throw new BusinessException(ExceptionEnum.PERMISSION_DENIED);
            }
        }
        
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
        RequestUserInfo userInfo = InvocationContextHolder.getInvocationContext().getRequestUserInfo();
        
        // 权限检查 - 组织管理员跳过权限检查
        if (!userInfo.getUserType().equals(UserTypeEnum.ORG_MANAGER_USER.getCode())) {
            if (!permissionChecker.hasTerminalGroupManagePermission(userInfo.getUid(), tgid)) {
                throw new BusinessException(ExceptionEnum.PERMISSION_DENIED);
            }
        }
        
        terminalGroupService.deleteTerminalGroup(tgid, userInfo.getUid());
    }

    @Transactional
    @SkipOrgManagerPermissionCheck
    public void updateTerminalGroup(UpdateTerminalGroupRequest request) {
        RequestUserInfo userInfo = InvocationContextHolder.getInvocationContext().getRequestUserInfo();
        
        // 权限检查 - 组织管理员跳过权限检查
        if (!userInfo.getUserType().equals(UserTypeEnum.ORG_MANAGER_USER.getCode())) {
            if (!permissionChecker.hasTerminalGroupManagePermission(userInfo.getUid(), request.getTgid())) {
                throw new BusinessException(ExceptionEnum.PERMISSION_DENIED);
            }
        }
        
        UpdateTerminalGroupDTO updateDTO = UpdateTerminalGroupDTO.builder()
                .tgid(request.getTgid())
                .terminalGroupName(request.getTerminalGroupName())
                .description(request.getDescription())
                .updatorId(userInfo.getUid())
                .build();
        
        terminalGroupService.updateTerminalGroup(updateDTO);
    }

    public PageVO<TerminalGroupListResponse> listTerminalGroup(PageRequestDTO<QueryTerminalGroupListRequest> requestDTO) {
        RequestUserInfo userInfo = InvocationContextHolder.getInvocationContext().getRequestUserInfo();
        
        QueryTerminalGroupListDTO queryDTO = QueryTerminalGroupListDTO.builder()
                .parentTgid(requestDTO.getRequest().getParentTgid())
                .terminalGroupName(requestDTO.getRequest().getTerminalGroupName())
                .oid(userInfo.getOid())
                .userId(userInfo.getUid())
                .build();
        
        PageVO<TerminalGroupListDTO> applicationResult = terminalGroupService.listTerminalGroup(requestDTO.getPageNum(), requestDTO.getPageSize(), queryDTO);
        
        // 转换Application层DTO到API层Response
        return PageVO.<TerminalGroupListResponse>builder()
                .pageNum(applicationResult.getPageNum())
                .pageSize(applicationResult.getPageSize())
                .total(applicationResult.getTotal())
                .pages(applicationResult.getPages())
                .list(applicationResult.getList().stream()
                        .map(this::convertToListResponse)
                        .collect(Collectors.toList()))
                .build();
    }
    
    private TerminalGroupListResponse convertToListResponse(TerminalGroupListDTO dto) {
        TerminalGroupListResponse response = new TerminalGroupListResponse();
        response.setTgid(dto.getTgid());
        response.setTerminalGroupName(dto.getTerminalGroupName());
        response.setParent(dto.getParent());
        response.setParentName(dto.getParentName());
        response.setDescription(dto.getDescription());
        response.setTgType(dto.getTgType());
        response.setChildrenCount(dto.getChildrenCount());
        response.setCreateTime(dto.getCreateTime());
        return response;
    }

    public PageVO<TerminalGroupListResponse> searchTerminalGroup(PageRequestDTO<SearchTerminalGroupRequest> requestDTO) {
        RequestUserInfo userInfo = InvocationContextHolder.getInvocationContext().getRequestUserInfo();
        
        SearchTerminalGroupDTO searchDTO = SearchTerminalGroupDTO.builder()
                .keyword(requestDTO.getRequest().getKeyword())
                .tgType(requestDTO.getRequest().getTgType())
                .oid(userInfo.getOid())
                .userId(userInfo.getUid())
                .build();
        
        return terminalGroupService.searchTerminalGroup(requestDTO.getPageNum(), requestDTO.getPageSize(), searchDTO);
    }

    public TerminalGroupDetailResponse getTerminalGroupDetail(Long tgid) {
        RequestUserInfo userInfo = InvocationContextHolder.getInvocationContext().getRequestUserInfo();
        
        // 权限检查
        if (!permissionChecker.hasTerminalGroupAccessPermission(userInfo.getUid(), tgid)) {
            throw new BusinessException(ExceptionEnum.PERMISSION_DENIED);
        }
        
        TerminalGroup terminalGroup = terminalGroupService.getTerminalGroupById(tgid);
        if (terminalGroup == null) {
            throw new BusinessException(ExceptionEnum.TERMINAL_GROUP_NOT_FOUND);
        }
        
        return terminalGroupConverter.terminalGroup2DetailResponse(terminalGroup);
    }

    public List<TerminalGroupDetailResponse> getChildrenTerminalGroups(Long tgid) {
        RequestUserInfo userInfo = InvocationContextHolder.getInvocationContext().getRequestUserInfo();
        
        // 权限检查
        if (!permissionChecker.hasTerminalGroupAccessPermission(userInfo.getUid(), tgid)) {
            throw new BusinessException(ExceptionEnum.PERMISSION_DENIED);
        }
        
        List<TerminalGroup> children = terminalGroupService.getChildrenTerminalGroups(tgid);
        return children.stream()
                .map(terminalGroupConverter::terminalGroup2DetailResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    @SkipOrgManagerPermissionCheck
    public void bindUserGroupToTerminalGroup(BindUserGroupRequest request) {
        RequestUserInfo userInfo = InvocationContextHolder.getInvocationContext().getRequestUserInfo();
        
        // 权限检查 - 组织管理员跳过权限检查
        if (!userInfo.getUserType().equals(UserTypeEnum.ORG_MANAGER_USER.getCode())) {
            if (!permissionChecker.hasTerminalGroupManagePermission(userInfo.getUid(), request.getTgid())) {
                throw new BusinessException(ExceptionEnum.PERMISSION_DENIED);
            }
        }
        
        BindUserGroupDTO bindDTO = BindUserGroupDTO.builder()
                .tgid(request.getTgid())
                .ugids(request.getUgids())
                .includeChildren(request.getIncludeChildren())
                .operatorId(userInfo.getUid())
                .build();
        
        bindingService.bindUserGroupToTerminalGroup(bindDTO);
    }

    @Transactional
    @SkipOrgManagerPermissionCheck
    public void unbindUserGroupFromTerminalGroup(Long tgid, Long ugid) {
        RequestUserInfo userInfo = InvocationContextHolder.getInvocationContext().getRequestUserInfo();
        
        // 权限检查 - 组织管理员跳过权限检查
        if (!userInfo.getUserType().equals(UserTypeEnum.ORG_MANAGER_USER.getCode())) {
            if (!permissionChecker.hasTerminalGroupManagePermission(userInfo.getUid(), tgid)) {
                throw new BusinessException(ExceptionEnum.PERMISSION_DENIED);
            }
        }
        
        bindingService.unbindUserGroupFromTerminalGroup(tgid, ugid, userInfo.getUid());
    }

    public PageVO<UserGroupBindingResponse> getTerminalGroupBindings(PageRequestDTO<QueryUserGroupBindingRequest> requestDTO) {
        RequestUserInfo userInfo = InvocationContextHolder.getInvocationContext().getRequestUserInfo();
        
        // 权限检查 - 组织管理员跳过权限检查
        if (!userInfo.getUserType().equals(UserTypeEnum.ORG_MANAGER_USER.getCode())) {
            if (!permissionChecker.hasTerminalGroupAccessPermission(userInfo.getUid(), requestDTO.getRequest().getTgid())) {
                throw new BusinessException(ExceptionEnum.PERMISSION_DENIED);
            }
        }
        
        QueryUserGroupBindingDTO queryDTO = QueryUserGroupBindingDTO.builder()
                .tgid(requestDTO.getRequest().getTgid())
                .userGroupName(requestDTO.getRequest().getUserGroupName())
                .build();
        
        return bindingService.getTerminalGroupBindings(requestDTO.getPageNum(), requestDTO.getPageSize(), queryDTO);
    }

    public List<TerminalGroupDetailResponse> getAccessibleTerminalGroups() {
        RequestUserInfo userInfo = InvocationContextHolder.getInvocationContext().getRequestUserInfo();
        
        List<TerminalGroup> accessibleGroups = terminalGroupService.getAccessibleTerminalGroups(userInfo.getUid(), userInfo.getOid());
        return accessibleGroups.stream()
                .map(terminalGroupConverter::terminalGroup2DetailResponse)
                .collect(Collectors.toList());
    }

    public TerminalGroupPermissionResponse checkTerminalGroupPermission(Long tgid) {
        RequestUserInfo userInfo = InvocationContextHolder.getInvocationContext().getRequestUserInfo();
        
        return terminalGroupService.checkTerminalGroupPermission(userInfo.getUid(), tgid);
    }

    public List<TerminalGroupPermissionResponse> batchCheckPermissions(BatchPermissionCheckRequest request) {
        RequestUserInfo userInfo = InvocationContextHolder.getInvocationContext().getRequestUserInfo();
        
        return terminalGroupService.batchCheckPermissions(userInfo.getUid(), request.getTgids());
    }

    public TerminalGroupStatisticsResponse getTerminalGroupStatistics(Long tgid) {
        RequestUserInfo userInfo = InvocationContextHolder.getInvocationContext().getRequestUserInfo();
        
        // 权限检查
        if (!permissionChecker.hasTerminalGroupAccessPermission(userInfo.getUid(), tgid)) {
            throw new BusinessException(ExceptionEnum.PERMISSION_DENIED);
        }
        
        return terminalGroupService.getTerminalGroupStatistics(tgid);
    }

    @Transactional
    public BatchOperationResponse batchOperateTerminalGroups(BatchTerminalGroupOperationRequest request) {
        RequestUserInfo userInfo = InvocationContextHolder.getInvocationContext().getRequestUserInfo();
        
        BatchTerminalGroupOperationDTO operationDTO = BatchTerminalGroupOperationDTO.builder()
                .operationType(request.getOperationType())
                .items(request.getItems())
                .operatorId(userInfo.getUid())
                .oid(userInfo.getOid())
                .build();
        
        return terminalGroupService.batchOperateTerminalGroups(operationDTO);
    }

    public PageVO<TerminalGroupHistoryResponse> getTerminalGroupHistory(PageRequestDTO<QueryTerminalGroupHistoryRequest> requestDTO) {
        RequestUserInfo userInfo = InvocationContextHolder.getInvocationContext().getRequestUserInfo();
        
        // 权限检查
        if (!permissionChecker.hasTerminalGroupAccessPermission(userInfo.getUid(), requestDTO.getRequest().getTgid())) {
            throw new BusinessException(ExceptionEnum.PERMISSION_DENIED);
        }
        
        QueryTerminalGroupHistoryDTO queryDTO = QueryTerminalGroupHistoryDTO.builder()
                .tgid(requestDTO.getRequest().getTgid())
                .operationType(requestDTO.getRequest().getOperationType())
                .operatorId(requestDTO.getRequest().getOperatorId())
                .build();
        
        return terminalGroupService.getTerminalGroupHistory(requestDTO.getPageNum(), requestDTO.getPageSize(), queryDTO);
    }

    private TerminalGroupTreeNode buildTerminalGroupTree(List<TerminalGroup> terminalGroups, Long oid) {
        if (CollectionUtils.isEmpty(terminalGroups)) {
            return null;
        }
        
        // 找到根节点
        TerminalGroup rootGroup = terminalGroups.stream()
                .filter(group -> group.getParent() == null || group.getParent().equals(0L))
                .findFirst()
                .orElse(null);
        
        if (rootGroup == null) {
            return null;
        }
        
        // 构建树形结构
        return buildTreeNode(rootGroup, terminalGroups);
    }

    private TerminalGroupTreeNode buildTreeNode(TerminalGroup group, List<TerminalGroup> allGroups) {
        TerminalGroupTreeNode node = TerminalGroupTreeNode.builder()
                .tgid(group.getTgid())
                .tgName(group.getName())
                .parent(group.getParent())
                .path(group.getPath())
                .description(group.getDescription())
                .build();
        
        // 构建子节点
        List<TerminalGroup> children = allGroups.stream()
                .filter(child -> group.getTgid().equals(child.getParent()))
                .collect(Collectors.toList());
        
        // 设置子终端组数量
        node.setChildrenCount(children.size());
        
        if (CollectionUtils.isNotEmpty(children)) {
            List<TerminalGroupTreeNode> childNodes = children.stream()
                    .map(child -> buildTreeNode(child, allGroups))
                    .collect(Collectors.toList());
            node.setChildren(childNodes);
        }
        
        return node;
    }
}