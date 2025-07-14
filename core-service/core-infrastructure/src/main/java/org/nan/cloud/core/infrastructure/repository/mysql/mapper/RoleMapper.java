package org.nan.cloud.core.infrastructure.repository.mysql.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.nan.cloud.core.infrastructure.repository.mysql.DO.RoleDO;

import java.util.List;
import java.util.Map;

@Mapper
public interface RoleMapper extends BaseMapper<RoleDO> {

    /**
     * 根据一组用户 ID 查询 casbin_rule 表中 ptype='g' 且 v0（uid）在列表内的所有 (uid, rid) 对
     */
    @Select({
            "<script>",
            "SELECT",
            "  v0 AS uid,",
            "  v1 AS rid",
            "FROM rbac_casbin_rules",
            "WHERE ptype = 'g'",
            "  AND v0 IN",
            "  <foreach collection='userIds' item='id' open='(' separator=',' close=')'>",
            "    #{id}",
            "  </foreach>",
            "</script>"
    })
    List<Map<String, Object>> getRolesByUserIds(@Param("userIds") List<Long> userIds);
}
