package org.nan.cloud.core.facade;

import lombok.RequiredArgsConstructor;
import org.nan.cloud.common.basic.exception.ExceptionEnum;
import org.nan.cloud.common.basic.model.PageRequestDTO;
import org.nan.cloud.common.basic.model.PageVO;
import org.nan.cloud.common.web.context.GenericInvocationContext;
import org.nan.cloud.common.web.context.InvocationContextHolder;
import org.nan.cloud.common.web.context.RequestUserInfo;
import org.nan.cloud.core.DTO.QueryUserListDTO;
import org.nan.cloud.core.DTO.UserGroupRelDTO;
import org.nan.cloud.core.api.DTO.common.OrganizationDTO;
import org.nan.cloud.core.api.DTO.common.RoleDTO;
import org.nan.cloud.core.api.DTO.common.UserGroupTreeNode;
import org.nan.cloud.core.api.DTO.req.QueryUserListRequest;
import org.nan.cloud.core.api.DTO.res.UserGroupTreeResponse;
import org.nan.cloud.core.api.DTO.res.UserListResponse;
import org.nan.cloud.core.domain.Organization;
import org.nan.cloud.core.domain.Role;
import org.nan.cloud.core.domain.User;
import org.nan.cloud.core.domain.UserGroup;
import org.nan.cloud.core.infrastructure.repository.mysql.DO.RoleDO;
import org.nan.cloud.core.service.*;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class UserGroupFacade {

    private final OrgService  orgService;

    private final UserGroupService userGroupService;

    private final UserService userService;

    private final RoleAndPermissionService roleAndPermissionService;

    private final PermissionChecker permissionChecker;

    public UserGroupTreeResponse getUserGroupTree() {
        RequestUserInfo requestUser = InvocationContextHolder.getContext().getRequestUser();

        Organization organization = orgService.getOrgById(requestUser.getOid());
        List<UserGroupRelDTO> userGroupRel = userGroupService.getAllUserGroupsByParent(requestUser.getUgid());
        UserGroupTreeNode rootNode = generateRootNode(requestUser.getUgid(), userGroupRel);

        UserGroupTreeResponse response = new UserGroupTreeResponse();
        response.setOrganization(new OrganizationDTO(organization.getOid(), organization.getName(), organization.getSuffix()));
        response.setRoot(rootNode);
        return response;
    }

    public PageVO<UserListResponse> listUser(PageRequestDTO<QueryUserListRequest> requestDTO) {
        Long ugid = InvocationContextHolder.getUgid();
        Long oid = InvocationContextHolder.getOid();
        ExceptionEnum.USER_GROUP_PERMISSION_DENIED.throwIf(!permissionChecker.ifHasPermissionOnTargetUserGroup(ugid, requestDTO.getParams().getUgid()));
        QueryUserListDTO dto = QueryUserListDTO.builder()
                .oid(oid)
                .ugid(ugid)
                .ifIncludeSubGroups(requestDTO.getParams().isIncludeSubGroups())
                .userNameKeyword(requestDTO.getParams().getUserNameKeyword())
                .emailKeyword(requestDTO.getParams().getEmailKeyword())
                .build();
        PageVO<User> userPageVO = userService.pageUsers(requestDTO.getPageNum(), requestDTO.getPageSize(), dto);
        List<Long> uids = userPageVO.getRecords().stream().map(User::getUid).toList();
        Map<Long, List<Role>> rolesMap = roleAndPermissionService.getRolesByUserIds(uids);
        return userPageVO.map(e -> UserListResponse.builder()
                .uid(e.getUid())
                .username(e.getUsername())
                .ugid(e.getUgid())
                .ugName(e.getUgName())
                .roles(rolesMap.get(e.getUid()).stream().map(r -> new RoleDTO(r.getRid(), r.getOid(), r.getName())).collect(Collectors.toSet()))
                .email(e.getEmail())
                .active(e.getStatus())
                .updateTime(e.getUpdateTime())
                .createTime(e.getCreateTime())
                .build());
    }

    private UserGroupTreeNode generateRootNode(Long rootUgid, List<UserGroupRelDTO> userGroupRel) {
        Map<Long, UserGroupTreeNode> nodeMap = userGroupRel.stream()
                .collect(Collectors.toMap(
                        UserGroupRelDTO::getUgid,
                        dto -> {
                            return UserGroupTreeNode.builder()
                                    .ugid(dto.getUgid())
                                    .ugName(dto.getUgName())
                                    .parent(dto.getParent())
                                    .path(dto.getPath())
                                    .pathMap(dto.getPathMap())
                                    .children(new ArrayList<>())
                                    .build();
                        }
                ));

        UserGroupTreeNode root = null;
        for (UserGroupTreeNode node : nodeMap.values()) {
            if (Objects.equals(node.getUgid(), rootUgid)) {
                root = node;
            }
            else {
                nodeMap.get(node.getParent()).getChildren().add(node);
            }
        }
        ExceptionEnum.USER_GROUP_INIT_FAILED.throwIf(root == null);
        return root;
    }


}
