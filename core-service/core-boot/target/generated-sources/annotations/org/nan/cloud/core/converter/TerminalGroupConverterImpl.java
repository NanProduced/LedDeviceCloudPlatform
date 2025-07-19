package org.nan.cloud.core.converter;

import javax.annotation.processing.Generated;
import org.nan.cloud.core.api.DTO.common.OrganizationDTO;
import org.nan.cloud.core.api.DTO.common.TerminalGroupTreeNode;
import org.nan.cloud.core.api.DTO.res.TerminalGroupDetailResponse;
import org.nan.cloud.core.api.DTO.res.TerminalGroupListResponse;
import org.nan.cloud.core.domain.Organization;
import org.nan.cloud.core.domain.TerminalGroup;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-07-20T02:34:59+0800",
    comments = "version: 1.6.3, compiler: javac, environment: Java 17.0.15 (Eclipse Adoptium)"
)
@Component
public class TerminalGroupConverterImpl implements TerminalGroupConverter {

    @Override
    public TerminalGroupDetailResponse terminalGroup2DetailResponse(TerminalGroup terminalGroup) {
        if ( terminalGroup == null ) {
            return null;
        }

        TerminalGroupDetailResponse terminalGroupDetailResponse = new TerminalGroupDetailResponse();

        if ( terminalGroup.getName() != null ) {
            terminalGroupDetailResponse.setTerminalGroupName( terminalGroup.getName() );
        }
        if ( terminalGroup.getTgid() != null ) {
            terminalGroupDetailResponse.setTgid( terminalGroup.getTgid() );
        }
        if ( terminalGroup.getOid() != null ) {
            terminalGroupDetailResponse.setOid( terminalGroup.getOid() );
        }
        if ( terminalGroup.getParent() != null ) {
            terminalGroupDetailResponse.setParent( terminalGroup.getParent() );
        }
        if ( terminalGroup.getPath() != null ) {
            terminalGroupDetailResponse.setPath( terminalGroup.getPath() );
        }
        if ( terminalGroup.getDescription() != null ) {
            terminalGroupDetailResponse.setDescription( terminalGroup.getDescription() );
        }
        if ( terminalGroup.getCreatorId() != null ) {
            terminalGroupDetailResponse.setCreatorId( terminalGroup.getCreatorId() );
        }
        if ( terminalGroup.getCreateTime() != null ) {
            terminalGroupDetailResponse.setCreateTime( terminalGroup.getCreateTime() );
        }
        if ( terminalGroup.getUpdaterId() != null ) {
            terminalGroupDetailResponse.setUpdaterId( terminalGroup.getUpdaterId() );
        }
        if ( terminalGroup.getUpdateTime() != null ) {
            terminalGroupDetailResponse.setUpdateTime( terminalGroup.getUpdateTime() );
        }

        return terminalGroupDetailResponse;
    }

    @Override
    public TerminalGroupListResponse terminalGroup2ListResponse(TerminalGroup terminalGroup) {
        if ( terminalGroup == null ) {
            return null;
        }

        TerminalGroupListResponse.TerminalGroupListResponseBuilder terminalGroupListResponse = TerminalGroupListResponse.builder();

        if ( terminalGroup.getName() != null ) {
            terminalGroupListResponse.terminalGroupName( terminalGroup.getName() );
        }
        if ( terminalGroup.getTgid() != null ) {
            terminalGroupListResponse.tgid( terminalGroup.getTgid() );
        }
        if ( terminalGroup.getParent() != null ) {
            terminalGroupListResponse.parent( terminalGroup.getParent() );
        }
        if ( terminalGroup.getDescription() != null ) {
            terminalGroupListResponse.description( terminalGroup.getDescription() );
        }
        if ( terminalGroup.getCreateTime() != null ) {
            terminalGroupListResponse.createTime( terminalGroup.getCreateTime() );
        }

        return terminalGroupListResponse.build();
    }

    @Override
    public OrganizationDTO organization2OrganizationDTO(Organization organization) {
        if ( organization == null ) {
            return null;
        }

        OrganizationDTO organizationDTO = new OrganizationDTO();

        if ( organization.getName() != null ) {
            organizationDTO.setOrgName( organization.getName() );
        }
        if ( organization.getOid() != null ) {
            organizationDTO.setOid( organization.getOid() );
        }
        if ( organization.getSuffix() != null ) {
            organizationDTO.setSuffix( organization.getSuffix() );
        }

        return organizationDTO;
    }

    @Override
    public TerminalGroupTreeNode terminalGroup2TreeNode(TerminalGroup terminalGroup) {
        if ( terminalGroup == null ) {
            return null;
        }

        TerminalGroupTreeNode.TerminalGroupTreeNodeBuilder terminalGroupTreeNode = TerminalGroupTreeNode.builder();

        if ( terminalGroup.getName() != null ) {
            terminalGroupTreeNode.tgName( terminalGroup.getName() );
        }
        if ( terminalGroup.getTgid() != null ) {
            terminalGroupTreeNode.tgid( terminalGroup.getTgid() );
        }
        if ( terminalGroup.getParent() != null ) {
            terminalGroupTreeNode.parent( terminalGroup.getParent() );
        }
        if ( terminalGroup.getPath() != null ) {
            terminalGroupTreeNode.path( terminalGroup.getPath() );
        }
        if ( terminalGroup.getDescription() != null ) {
            terminalGroupTreeNode.description( terminalGroup.getDescription() );
        }

        return terminalGroupTreeNode.build();
    }
}
