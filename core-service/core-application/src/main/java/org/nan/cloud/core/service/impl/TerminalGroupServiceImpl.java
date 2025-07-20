package org.nan.cloud.core.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.nan.cloud.common.basic.model.PageVO;
import org.nan.cloud.core.DTO.*;
import org.nan.cloud.core.domain.TerminalGroup;
import org.nan.cloud.core.enums.TerminalGroupTypeEnum;
import org.nan.cloud.core.repository.TerminalGroupRepository;
import org.nan.cloud.core.repository.UserGroupTerminalGroupBindingRepository;
import org.nan.cloud.core.service.BusinessCacheService;
import org.nan.cloud.core.service.TerminalGroupService;
import org.nan.cloud.core.service.UserGroupTerminalGroupBindingService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TerminalGroupServiceImpl implements TerminalGroupService {

    private final TerminalGroupRepository terminalGroupRepository;
    private final BusinessCacheService businessCacheService;

    @Override
    public TerminalGroup getTerminalGroupById(Long tgid) {
        return terminalGroupRepository.getTerminalGroupById(tgid);
    }

    @Override
    public TerminalGroup getTerminalGroupById(Long tgid, Long orgId) {
        // 1. 尝试从缓存获取
        TerminalGroup cachedGroup = businessCacheService.getTerminalGroup(tgid, orgId, TerminalGroup.class);
        if (cachedGroup != null) {
            log.debug("终端组缓存命中: tgid={}, orgId={}", tgid, orgId);
            return cachedGroup;
        }
        
        // 2. 缓存未命中，从数据库加载
        TerminalGroup terminalGroup = terminalGroupRepository.getTerminalGroupById(tgid);
        if (terminalGroup != null) {
            // 3. 将结果存入缓存
            businessCacheService.cacheTerminalGroup(terminalGroup, tgid, orgId);
            log.debug("终端组已缓存: tgid={}, orgId={}, name={}", tgid, orgId, terminalGroup.getName());
        }
        
        return terminalGroup;
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
    public void deleteTerminalGroup(Long tgid, Long orgId, Long operatorId) {
        // TODO: 检查是否有子终端组
        // TODO: 检查是否有绑定的设备
        
        // 1. 删除数据库记录
        terminalGroupRepository.deleteTerminalGroup(tgid);
        
        // 2. 清理缓存
        businessCacheService.evictTerminalGroup(tgid, orgId);
        log.info("终端组删除并清理缓存: tgid={}, orgId={}, operatorId={}", tgid, orgId, operatorId);
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
        terminalGroupRepository.updateTerminalGroup(existingGroup);
    }

    @Override
    @Transactional
    public void updateTerminalGroup(UpdateTerminalGroupDTO updateTerminalGroupDTO, Long orgId) {
        // 1. 更新数据库
        TerminalGroup existingGroup = terminalGroupRepository.getTerminalGroupById(updateTerminalGroupDTO.getTgid());
        boolean updated = false;
        
        if (StringUtils.isNotBlank(updateTerminalGroupDTO.getTerminalGroupName())) {
            existingGroup.setName(updateTerminalGroupDTO.getTerminalGroupName());
            updated = true;
        }
        if (StringUtils.isNotBlank(updateTerminalGroupDTO.getDescription())) {
            existingGroup.setDescription(updateTerminalGroupDTO.getDescription());
            updated = true;
        }
        
        if (updated) {
            terminalGroupRepository.updateTerminalGroup(existingGroup);
            
            // 2. 更新缓存
            businessCacheService.cacheTerminalGroup(existingGroup, updateTerminalGroupDTO.getTgid(), orgId);
            log.info("终端组更新并刷新缓存: tgid={}, orgId={}, name={}", 
                updateTerminalGroupDTO.getTgid(), orgId, existingGroup.getName());
        }
    }


    @Override
    public List<TerminalGroup> getChildGroups(Long parentTgid) {
        return terminalGroupRepository.getChildTerminalGroups(parentTgid);
    }
}