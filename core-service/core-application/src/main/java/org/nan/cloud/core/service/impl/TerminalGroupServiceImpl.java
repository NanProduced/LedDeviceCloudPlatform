package org.nan.cloud.core.service.impl;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.nan.cloud.common.basic.model.PageVO;
import org.nan.cloud.core.DTO.*;
import org.nan.cloud.core.domain.TerminalGroup;
import org.nan.cloud.core.enums.TerminalGroupTypeEnum;
import org.nan.cloud.core.repository.TerminalGroupRepository;
import org.nan.cloud.core.repository.UserGroupTerminalGroupBindingRepository;
import org.nan.cloud.core.service.TerminalGroupService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TerminalGroupServiceImpl implements TerminalGroupService {

    private final TerminalGroupRepository terminalGroupRepository;
    private final UserGroupTerminalGroupBindingRepository bindingRepository;

    @Override
    public TerminalGroup getTerminalGroupById(Long tgid) {
        return terminalGroupRepository.getTerminalGroupById(tgid);
    }

    @Override
    @Transactional
    public void createTerminalGroup(CreateTerminalGroupDTO createTerminalGroupDTO) {
        TerminalGroup parentGroup = terminalGroupRepository.getTerminalGroupById(createTerminalGroupDTO.getParentTgid());
        TerminalGroup terminalGroup = TerminalGroup.builder()
                .name(createTerminalGroupDTO.getTerminalGroupName())
                .oid(createTerminalGroupDTO.getOid())
                .parent(createTerminalGroupDTO.getParentTgid())
                .path(parentGroup.getPath())
                .description(createTerminalGroupDTO.getDescription())
                .tgType(TerminalGroupTypeEnum.NORMAL_GROUP.getType())
                .creatorId(createTerminalGroupDTO.getCreatorId())
                .build();
        terminalGroupRepository.createTerminalGroup(terminalGroup);
    }

    @Override
    @Transactional
    public void deleteTerminalGroup(Long tgid, Long operatorId) {
        // TODO: 检查是否有子终端组
        // TODO: 检查是否有绑定的设备
        terminalGroupRepository.deleteTerminalGroup(tgid);
    }

    @Override
    @Transactional
    public void updateTerminalGroup(UpdateTerminalGroupDTO updateTerminalGroupDTO) {
        TerminalGroup existingGroup = terminalGroupRepository.getTerminalGroupById(updateTerminalGroupDTO.getTgid());
        if (StringUtils.isNotBlank(updateTerminalGroupDTO.getTerminalGroupName())) {
            existingGroup.setName(updateTerminalGroupDTO.getTerminalGroupName());
        }
        if (StringUtils.isNotBlank(updateTerminalGroupDTO.getDescription())) {
            existingGroup.setDescription(updateTerminalGroupDTO.getDescription());
        }
        existingGroup.setDescription(updateTerminalGroupDTO.getDescription());
        terminalGroupRepository.updateTerminalGroup(existingGroup);
    }

    @Override
    public PageVO<TerminalGroup> searchAccessibleTerminalGroups(Integer pageNum, Integer pageSize, SearchTerminalGroupDTO searchDTO) {
        // 获取用户组可访问的终端组ID列表
        List<Long> accessibleTerminalGroupIds = bindingRepository.getAccessibleTerminalGroupIds(searchDTO.getUgid());
        
        // 如果没有可访问的终端组，直接返回空结果
        if (accessibleTerminalGroupIds.isEmpty()) {
            return PageVO.empty();
        }
        
        // 根据关键词和权限过滤终端组
        return terminalGroupRepository.searchAccessibleTerminalGroups(pageNum, pageSize, searchDTO, accessibleTerminalGroupIds);
    }

    @Override
    public List<TerminalGroup> getChildGroups(Long parentTgid) {
        return List.of();
    }
}