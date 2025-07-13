package org.nan.cloud.core.facade;

import lombok.RequiredArgsConstructor;
import org.nan.cloud.common.basic.exception.ExceptionEnum;
import org.nan.cloud.common.web.context.GenericInvocationContext;
import org.nan.cloud.common.web.context.InvocationContextHolder;
import org.nan.cloud.common.web.context.RequestUserInfo;
import org.nan.cloud.core.DTO.UserGroupRelDTO;
import org.nan.cloud.core.api.DTO.common.OrganizationDTO;
import org.nan.cloud.core.api.DTO.common.UserGroupTreeNode;
import org.nan.cloud.core.api.DTO.res.UserGroupTreeResponse;
import org.nan.cloud.core.domain.Organization;
import org.nan.cloud.core.domain.UserGroup;
import org.nan.cloud.core.service.OrgService;
import org.nan.cloud.core.service.UserGroupService;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class UserGroupFacade {

    private final OrgService  orgService;

    private final UserGroupService userGroupService;

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
            if (node.getUgid() == rootUgid) {
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
