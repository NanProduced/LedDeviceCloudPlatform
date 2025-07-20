package org.nan.cloud.core.infrastructure.repository.mysql.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.*;
import org.nan.cloud.core.infrastructure.repository.mysql.DO.UserGroupTerminalGroupBindingDO;

import java.util.List;

@Mapper
public interface UserGroupTerminalGroupBindingMapper extends BaseMapper<UserGroupTerminalGroupBindingDO> {

    /**
     * 检查用户组是否有终端组权限（基于INCLUDE/EXCLUDE绑定类型精确计算）
     * 使用CTE优化的完整权限计算逻辑
     */
    @Select({
            "WITH included_terminals AS (",
            "  SELECT DISTINCT tg.tgid",
            "  FROM user_group_terminal_group_rel b",
            "  JOIN terminal_group tg ON (",
            "    (b.include_sub = 1 AND (",
            "      tg.path LIKE CONCAT(",
            "        (SELECT path FROM terminal_group WHERE tgid = b.tgid), '|%'",
            "      )",
            "      OR tg.tgid = b.tgid",
            "    ))",
            "    OR (b.include_sub = 0 AND tg.tgid = b.tgid)",
            "  )",
            "  WHERE b.ugid = #{ugid} AND b.binding_type = 'INCLUDE'",
            "),",
            "excluded_terminals AS (",
            "  SELECT DISTINCT tg.tgid",
            "  FROM user_group_terminal_group_rel b",
            "  JOIN terminal_group tg ON (",
            "    (b.include_sub = 1 AND (",
            "      tg.path LIKE CONCAT(",
            "        (SELECT path FROM terminal_group WHERE tgid = b.tgid), '|%'",
            "      )",
            "      OR tg.tgid = b.tgid",
            "    ))",
            "    OR (b.include_sub = 0 AND tg.tgid = b.tgid)",
            "  )",
            "  WHERE b.ugid = #{ugid} AND b.binding_type = 'EXCLUDE'",
            ")",
            "SELECT CASE ",
            "  WHEN EXISTS(SELECT 1 FROM included_terminals WHERE tgid = #{tgid})",
            "   AND NOT EXISTS(SELECT 1 FROM excluded_terminals WHERE tgid = #{tgid})",
            "  THEN 1 ELSE 0 END"
    })
    boolean hasTerminalGroupPermission(@Param("ugid") Long ugid, @Param("tgid") Long tgid);

    /**
     * 批量权限校验（高性能版本）
     * 一次查询返回多个终端组的权限状态
     */
    @Select({
            "<script>",
            "WITH included_terminals AS (",
            "  SELECT DISTINCT tg.tgid",
            "  FROM user_group_terminal_group_rel b",
            "  JOIN terminal_group tg ON (",
            "    (b.include_sub = 1 AND (",
            "      tg.path LIKE CONCAT(",
            "        (SELECT path FROM terminal_group WHERE tgid = b.tgid), '|%'",
            "      )",
            "      OR tg.tgid = b.tgid",
            "    ))",
            "    OR (b.include_sub = 0 AND tg.tgid = b.tgid)",
            "  )",
            "  WHERE b.ugid = #{ugid} AND b.binding_type = 'INCLUDE'",
            "),",
            "excluded_terminals AS (",
            "  SELECT DISTINCT tg.tgid",
            "  FROM user_group_terminal_group_rel b",
            "  JOIN terminal_group tg ON (",
            "    (b.include_sub = 1 AND (",
            "      tg.path LIKE CONCAT(",
            "        (SELECT path FROM terminal_group WHERE tgid = b.tgid), '|%'",
            "      )",
            "      OR tg.tgid = b.tgid",
            "    ))",
            "    OR (b.include_sub = 0 AND tg.tgid = b.tgid)",
            "  )",
            "  WHERE b.ugid = #{ugid} AND b.binding_type = 'EXCLUDE'",
            ")",
            "SELECT tgid, ",
            "  CASE ",
            "    WHEN EXISTS(SELECT 1 FROM included_terminals i WHERE i.tgid = t.tgid)",
            "     AND NOT EXISTS(SELECT 1 FROM excluded_terminals e WHERE e.tgid = t.tgid)",
            "    THEN 1 ELSE 0 END as has_permission",
            "FROM (",
            "  <foreach collection='tgids' item='tgid' separator=' UNION ALL '>",
            "    SELECT #{tgid} as tgid",
            "  </foreach>",
            ") t",
            "</script>"
    })
    @Results({
            @Result(property = "tgid", column = "tgid"),
            @Result(property = "hasPermission", column = "has_permission")
    })
    List<TerminalGroupPermissionResult> batchCheckPermissions(@Param("ugid") Long ugid, @Param("tgids") List<Long> tgids);
    
    /**
     * 权限校验结果
     */
    class TerminalGroupPermissionResult {
        private Long tgid;
        private Boolean hasPermission;
        
        public Long getTgid() { return tgid; }
        public void setTgid(Long tgid) { this.tgid = tgid; }
        public Boolean getHasPermission() { return hasPermission; }
        public void setHasPermission(Boolean hasPermission) { this.hasPermission = hasPermission; }
    }

    /**
     * 批量插入绑定（支持新的binding_type字段）
     */
    @Insert({
            "<script>",
            "INSERT INTO user_group_terminal_group_rel (ugid, tgid, include_sub, binding_type, oid, creator_id, create_time, updater_id, update_time)",
            "VALUES",
            "<foreach collection='bindings' item='item' separator=','>",
            "(#{item.ugid}, #{item.tgid}, #{item.includeSub}, #{item.bindingType,typeHandler=org.nan.cloud.core.infrastructure.config.BindingTypeHandler}, #{item.oid}, #{item.creatorId}, #{item.createTime}, #{item.updaterId}, #{item.updateTime})",
            "</foreach>",
            "</script>"
    })
    int insertBatchSomeColumn(@Param("bindings") List<UserGroupTerminalGroupBindingDO> bindings);

    /**
     * 获取用户组权限绑定详细信息（包含终端组信息和binding_type）
     */
    @Select({
            "SELECT b.*, tg.name as terminal_group_name, tg.path as terminal_group_path,",
            "       tg.parent as parent_tgid, ",
            "       LENGTH(tg.path) - LENGTH(REPLACE(tg.path, '|', '')) as depth,",
            "       (SELECT COUNT(*) FROM terminal_group WHERE parent = b.tgid) as child_count",
            "FROM user_group_terminal_group_rel b",
            "LEFT JOIN terminal_group tg ON b.tgid = tg.tgid",
            "WHERE b.ugid = #{ugid}",
            "ORDER BY b.binding_type DESC, LENGTH(tg.path) - LENGTH(REPLACE(tg.path, '|', '')), tg.path"
    })
    @Results({
            @Result(property = "bindingId", column = "binding_id"),
            @Result(property = "ugid", column = "ugid"),
            @Result(property = "tgid", column = "tgid"),
            @Result(property = "includeSub", column = "include_sub"),
            @Result(property = "bindingType", column = "binding_type", typeHandler = org.nan.cloud.core.infrastructure.config.BindingTypeHandler.class),
            @Result(property = "oid", column = "oid"),
            @Result(property = "creatorId", column = "creator_id"),
            @Result(property = "createTime", column = "create_time"),
            @Result(property = "updaterId", column = "updater_id"),
            @Result(property = "updateTime", column = "update_time"),
            @Result(property = "terminalGroupName", column = "terminal_group_name"),
            @Result(property = "terminalGroupPath", column = "terminal_group_path"),
            @Result(property = "parentTgid", column = "parent_tgid"),
            @Result(property = "depth", column = "depth"),
            @Result(property = "childCount", column = "child_count")
    })
    List<UserGroupTerminalGroupBindingDO> selectUserGroupPermissionDetails(@Param("ugid") Long ugid);
}