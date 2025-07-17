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
     * 获取用户可访问的终端组列表
     */
    List<TerminalGroup> getAccessibleTerminalGroups(Long userId, Long oid);

    /**
     * 分页查询终端组列表
     */
    PageVO<TerminalGroupListDTO> listTerminalGroup(Integer pageNum, Integer pageSize, QueryTerminalGroupListDTO queryDTO);

    /**
     * 搜索终端组
     */
    PageVO<TerminalGroupListDTO> searchTerminalGroup(Integer pageNum, Integer pageSize, SearchTerminalGroupDTO searchDTO);

    /**
     * 获取子终端组
     */
    List<TerminalGroup> getChildrenTerminalGroups(Long tgid);

    /**
     * 检查终端组权限
     */
    TerminalGroupPermissionDTO checkTerminalGroupPermission(Long userId, Long tgid);

    /**
     * 批量检查终端组权限
     */
    List<TerminalGroupPermissionDTO> batchCheckPermissions(Long userId, List<Long> tgids);

    /**
     * 获取终端组统计信息
     */
    TerminalGroupStatisticsDTO getTerminalGroupStatistics(Long tgid);

    /**
     * 批量操作终端组
     */
    BatchOperationDTO batchOperateTerminalGroups(BatchTerminalGroupOperationDTO operationDTO);

    /**
     * 获取终端组操作历史
     */
    PageVO<TerminalGroupHistoryDTO> getTerminalGroupHistory(Integer pageNum, Integer pageSize, QueryTerminalGroupHistoryDTO queryDTO);

    /**
     * 查询组下所有终端组（包含当前组）
     */
    List<TerminalGroupRelDTO> getAllTerminalGroupsByParent(Long tgid);
}