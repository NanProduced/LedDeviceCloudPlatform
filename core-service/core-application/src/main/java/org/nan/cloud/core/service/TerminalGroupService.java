package org.nan.cloud.core.service;

import org.nan.cloud.common.basic.model.PageVO;
import org.nan.cloud.core.DTO.*;
import org.nan.cloud.core.domain.TerminalGroup;

import java.util.List;

public interface TerminalGroupService {

    /**
     * 根据ID获取终端组
     */
    TerminalGroup getTerminalGroupById(Long tgid);
    
    /**
     * 根据ID和组织ID获取终端组（带缓存）
     */
    TerminalGroup getTerminalGroupById(Long tgid, Long orgId);

    /**
     * 创建终端组
     */
    void createTerminalGroup(CreateTerminalGroupDTO createTerminalGroupDTO);

    /**
     * 删除终端组
     */
    void deleteTerminalGroup(Long tgid, Long operatorId);
    
    /**
     * 删除终端组（带缓存清理）
     */
    void deleteTerminalGroup(Long tgid, Long orgId, Long operatorId);

    /**
     * 更新终端组
     */
    void updateTerminalGroup(UpdateTerminalGroupDTO updateTerminalGroupDTO);
    
    /**
     * 更新终端组（带缓存清理）
     */
    void updateTerminalGroup(UpdateTerminalGroupDTO updateTerminalGroupDTO, Long orgId);

    /**
     * 获取终端组的所有子组
     */
    List<TerminalGroup> getChildGroups(Long parentTgid);
}