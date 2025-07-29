package org.nan.cloud.core.repository;

import org.nan.cloud.common.basic.model.PageVO;
import org.nan.cloud.core.DTO.SearchTerminalGroupDTO;
import org.nan.cloud.core.domain.TerminalGroup;

import java.util.List;
import java.util.Set;

public interface TerminalGroupRepository {

    /**
     * 创建终端组
     */
    TerminalGroup createTerminalGroup(TerminalGroup terminalGroup);

    /**
     * 根据ID获取终端组
     */
    TerminalGroup getTerminalGroupById(Long tgid);

    List<TerminalGroup> getTerminalGroupNames(Long oid, Set<Long> tgids);

    /**
     * 更新终端组
     */
    void updateTerminalGroup(TerminalGroup terminalGroup);

    /**
     * 删除终端组
     */
    void deleteTerminalGroup(Long tgid);

    /**
     * 是否为同一组织
     * @param oid
     * @param tgid
     * @return
     */
    boolean ifTheSameOrg(Long oid, Long tgid);

    boolean ifTheSameOrg(Long oid, List<Long> tgids);
    
    /**
     * 根据权限和关键词搜索终端组
     */
    PageVO<TerminalGroup> searchAccessibleTerminalGroups(Integer pageNum, Integer pageSize, SearchTerminalGroupDTO searchDTO, List<Long> accessibleTerminalGroupIds);

    List<TerminalGroup> getChildTerminalGroups(Long parentTgid);

    String getPathByTgid(Long tgid);

    Set<Long> getAllTgidsByParent(Long tgid);
}
