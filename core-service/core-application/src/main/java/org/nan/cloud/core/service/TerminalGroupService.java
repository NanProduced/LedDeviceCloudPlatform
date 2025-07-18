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
     * 创建终端组
     */
    void createTerminalGroup(CreateTerminalGroupDTO createTerminalGroupDTO);

    /**
     * 删除终端组
     */
    void deleteTerminalGroup(Long tgid, Long operatorId);

    /**
     * 更新终端组
     */
    void updateTerminalGroup(UpdateTerminalGroupDTO updateTerminalGroupDTO);

    /**
     * 搜索用户可访问的终端组
     */
    PageVO<TerminalGroup> searchAccessibleTerminalGroups(Integer pageNum, Integer pageSize, SearchTerminalGroupDTO searchDTO);

    /**
     * 获取终端组的所有子组
     */
    List<TerminalGroup> getChildGroups(Long parentTgid);
}