package org.nan.cloud.core.infrastructure.repository.mysql.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.nan.cloud.core.infrastructure.repository.mysql.DO.RoleDO;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@Mapper
public interface RoleMapper extends BaseMapper<RoleDO> {

    @Select({
            "SELECT v1",
            " FROM rbac_casbin_rules",
            " WHERE ptype = 'g'",
            " AND v0 = #{uid};"
    })
    List<Long> getRidsByUid(@Param("uid") Long uid);

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

    @Select({
            "SELECT v0 AS user_id",
            " FROM rbac_casbin_rules",
            " WHERE ptype = 'g'",
            " AND v2 = #{oid}",
            " GROUP BY v0, v2",
            " HAVING COUNT(DISTINCT v1) = 1",
            " AND MAX(v1) = #{rid};"
    })
    List<Long> getUserWithOnlyRole(@Param("oid") Long oid, @Param("rid") Long rid);


    /**
     * 查询同一 oid 下，权限全集被 ridList 覆盖（包含自身）的所有角色
     */
    @Select("<script>\n" +
            "WITH input_perms AS (\n" +
            "  SELECT DISTINCT permission_id\n" +
            "  FROM role_permission_rel\n" +
            "  WHERE rid IN\n" +
            "  <foreach collection='ridList' item='rid' open='(' separator=',' close=')'>\n" +
            "    #{rid}\n" +
            "  </foreach>\n" +
            "),\n" +
            "candidate_roles AS (\n" +
            "  SELECT rid\n" +
            "  FROM role\n" +
            "  WHERE oid = #{oid}\n" +
            "  AND `type` != 0\n" +
            ")\n" +
            "SELECT r.*\n" +
            "FROM role r\n" +
            "JOIN candidate_roles cr ON cr.rid = r.rid\n" +
            "WHERE NOT EXISTS (\n" +
            "  SELECT 1\n" +
            "  FROM role_permission_rel rp\n" +
            "  WHERE rp.rid = r.rid\n" +
            "    AND rp.permission_id NOT IN (\n" +
            "      SELECT permission_id FROM input_perms\n" +
            "    )\n" +
            ")\n" +
            "</script>")
    List<RoleDO> selectCoveredRoles(
            @Param("ridList") Collection<Long> ridList,
            @Param("oid") Long oid
    );
}
