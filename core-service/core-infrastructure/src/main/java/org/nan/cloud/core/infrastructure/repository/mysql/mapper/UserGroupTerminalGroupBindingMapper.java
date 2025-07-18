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
     * 分页查询终端组绑定的用户组
     */
    @Select({
            "<script>",
            "SELECT b.*, ug.name as user_group_name",
            "FROM user_group_terminal_group_rel b",
            "LEFT JOIN user_group ug ON b.ugid = ug.ugid",
            "WHERE b.tgid = #{tgid}",
            "<if test='userGroupName != null and userGroupName != \"\"'>",
            "  AND ug.name LIKE CONCAT('%', #{userGroupName}, '%')",
            "</if>",
            "ORDER BY b.create_time DESC",
            "</script>"
    })
    IPage<UserGroupTerminalGroupBindingDO> selectBindingsByTerminalGroup(
            Page<?> page, 
            @Param("tgid") Long tgid, 
            @Param("userGroupName") String userGroupName);

    /**
     * 根据用户组ID获取绑定的终端组ID列表
     */
    @Select("SELECT tgid FROM user_group_terminal_group_rel WHERE ugid = #{ugid}")
    List<Long> selectTerminalGroupIdsByUserGroup(@Param("ugid") Long ugid);

    /**
     * 根据终端组ID获取绑定的用户组ID列表
     */
    @Select("SELECT ugid FROM user_group_terminal_group_rel WHERE tgid = #{tgid}")
    List<Long> selectUserGroupIdsByTerminalGroup(@Param("tgid") Long tgid);

    /**
     * 检查用户组是否有终端组权限（包含子组）
     */
    @Select({
            "SELECT COUNT(*) > 0",
            "FROM user_group_terminal_group_rel b",
            "JOIN terminal_group tg ON (",
            "  (b.include_sub = 1 AND (",
            "    tg.path LIKE CONCAT(",
            "      (SELECT path FROM terminal_group WHERE tgid = b.tgid), '|%'",
            "    )",
            "    OR tg.tgid = b.tgid",
            "  ))",
            "  OR tg.tgid = b.tgid",
            ")",
            "WHERE b.ugid = #{ugid} AND tg.tgid = #{tgid}"
    })
    boolean hasTerminalGroupPermission(@Param("ugid") Long ugid, @Param("tgid") Long tgid);

    /**
     * 获取用户组可访问的终端组ID列表
     */
    @Select({
            "SELECT DISTINCT tg.tgid",
            "FROM user_group_terminal_group_rel b",
            "JOIN terminal_group tg ON (",
            "  (b.include_sub = 1 AND (",
            "    tg.path LIKE CONCAT(",
            "      (SELECT path FROM terminal_group WHERE tgid = b.tgid), '|%'",
            "    )",
            "    OR tg.tgid = b.tgid",
            "  ))",
            "  OR tg.tgid = b.tgid",
            ")",
            "WHERE b.ugid = #{ugid}"
    })
    List<Long> selectAccessibleTerminalGroupIds(@Param("ugid") Long ugid);

    /**
     * 删除终端组的所有绑定
     */
    @Delete("DELETE FROM user_group_terminal_group_rel WHERE tgid = #{tgid}")
    int deleteByTerminalGroupId(@Param("tgid") Long tgid);

    /**
     * 删除用户组的所有绑定
     */
    @Delete("DELETE FROM user_group_terminal_group_rel WHERE ugid = #{ugid}")
    int deleteByUserGroupId(@Param("ugid") Long ugid);

    /**
     * 批量插入绑定
     */
    @Insert({
            "<script>",
            "INSERT INTO user_group_terminal_group_rel (ugid, tgid, include_sub, oid, creator_id, create_time, updater_id, update_time)",
            "VALUES",
            "<foreach collection='bindings' item='item' separator=','>",
            "(#{item.ugid}, #{item.tgid}, #{item.includeSub}, #{item.oid}, #{item.creatorId}, #{item.createTime}, #{item.updaterId}, #{item.updateTime})",
            "</foreach>",
            "</script>"
    })
    int batchInsert(@Param("bindings") List<UserGroupTerminalGroupBindingDO> bindings);
}