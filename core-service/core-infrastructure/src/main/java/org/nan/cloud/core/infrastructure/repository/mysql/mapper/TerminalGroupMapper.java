package org.nan.cloud.core.infrastructure.repository.mysql.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.nan.cloud.common.basic.model.PageVO;
import org.nan.cloud.core.DTO.SearchTerminalGroupDTO;
import org.nan.cloud.core.DTO.TerminalGroupListDTO;
import org.nan.cloud.core.infrastructure.repository.mysql.DO.TerminalGroupDO;

import java.util.List;

@Mapper
public interface TerminalGroupMapper extends BaseMapper<TerminalGroupDO> {
    
    /**
     * 根据权限和关键词搜索终端组
     */
    @Select({
            "<script>",
            "SELECT tg.tgid, tg.name as terminalGroupName, tg.parent, ",
            "       pt.name as parentName, tg.description, tg.tg_type as tgType, ",
            "       tg.create_time as createTime, ",
            "       (SELECT COUNT(*) FROM terminal_group WHERE parent = tg.tgid) as childrenCount ",
            "FROM terminal_group tg ",
            "LEFT JOIN terminal_group pt ON tg.parent = pt.tgid ",
            "WHERE tg.oid = #{searchDTO.oid} ",
            "  AND tg.tgid IN ",
            "  <foreach collection='accessibleTerminalGroupIds' item='tgid' open='(' close=')' separator=','>",
            "    #{tgid}",
            "  </foreach>",
            "  <if test='searchDTO.keyword != null and searchDTO.keyword != \"\"'>",
            "    AND tg.name LIKE CONCAT('%', #{searchDTO.keyword}, '%')",
            "  </if>",
            "  <if test='searchDTO.tgType != null'>",
            "    AND tg.tg_type = #{searchDTO.tgType}",
            "  </if>",
            "ORDER BY tg.create_time DESC",
            "</script>"
    })
    IPage<TerminalGroupListDTO> selectAccessibleTerminalGroups(
            Page<?> page,
            @Param("searchDTO") SearchTerminalGroupDTO searchDTO,
            @Param("accessibleTerminalGroupIds") List<Long> accessibleTerminalGroupIds);

}
