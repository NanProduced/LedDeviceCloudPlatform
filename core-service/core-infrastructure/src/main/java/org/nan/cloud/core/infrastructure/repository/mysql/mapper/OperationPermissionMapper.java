package org.nan.cloud.core.infrastructure.repository.mysql.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.*;
import org.nan.cloud.core.infrastructure.repository.mysql.DO.OperationPermissionDO;

import java.util.List;
import java.util.Set;

@Mapper
public interface OperationPermissionMapper extends BaseMapper<OperationPermissionDO> {

    /**
     * 给角色批量添加操作权限关系
     * @param rid           角色 ID
     * @param opIds         操作权限 ID 列表
     * @return 插入的记录数
     */
    @Insert({
            "<script>",
            "INSERT INTO role_operation_rel (rid, operation_permission_id) VALUES",
            "<foreach collection='opIds' item='opId' index='idx' separator=','>",
            "(#{rid}, #{opId})",
            "</foreach>",
            "</script>"
    })
    int insertRoleOperationPermissionRel(
            @Param("rid") Long rid,
            @Param("opIds") Set<Long> opIds
    );

    @Delete("DELETE FROM role_operation_rel WHERE rid = #{rid}")
    int deleteRoleOperationPermissionRel(@Param("rid") Long rid);

    @Select({
            "SELECT operation_permission_id",
            " FROM role_operation_rel",
            " WHERE rid = #{rid};"
    })
    List<Long> getOperationPermissionIdByRid(Long rid);

    @Select({
            "<script>",
            "SELECT DISTINCT operation_permission_id",
            " FROM role_operation_rel",
            "   <if test='rids != null and !rids.isEmpty()'>",
            "     WHERE rid IN",
            "     <foreach collection='rids' item='rid' open='(' separator=',' close=')'>",
            "       #{rid}",
            "     </foreach>",
            "   </if>",
            "</script>"
    })
    List<Long> getOperationPermissionIdByRids(List<Long> rids);
}
