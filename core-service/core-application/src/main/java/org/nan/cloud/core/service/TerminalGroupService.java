package org.nan.cloud.core.service;

import org.nan.cloud.core.DTO.*;
import org.nan.cloud.core.domain.TerminalGroup;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface TerminalGroupService {

    /**
     * 根据ID获取终端组
     */
    TerminalGroup getTerminalGroupById(Long tgid);

    Map<Long, String> getTgidAndTgNameMap(Long oid, Set<Long> tgid);
    
    /**
     * 根据ID和组织ID获取终端组（带缓存）
     */
    TerminalGroup getTerminalGroupById(Long tgid, Long orgId);

    /**
     * 创建终端组
     * @param createTerminalGroupDTO 创建参数
     * @return 创建的终端组
     */
    TerminalGroup createTerminalGroup(CreateTerminalGroupDTO createTerminalGroupDTO);

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