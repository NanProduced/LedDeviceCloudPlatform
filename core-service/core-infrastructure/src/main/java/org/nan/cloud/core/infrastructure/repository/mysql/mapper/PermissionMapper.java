package org.nan.cloud.core.infrastructure.repository.mysql.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.nan.cloud.core.infrastructure.repository.mysql.DO.PermissionDO;

import java.util.List;
import java.util.Set;

@Mapper
public interface PermissionMapper extends BaseMapper<PermissionDO> {

    /**
     * 根据单个用户 ID，查询该用户所拥有的权限id
     */
    @Select(
            "SELECT DISTINCT p.* " +
            "  FROM rbac_casbin_rule cr " +
            "  JOIN role                 r  ON r.rid = cr.v1 " +
            "  JOIN role_permission_rel  rp ON rp.rid = r.rid " +
            "  JOIN permission           p  ON p.permission_id = rp.permission_id " +
            " WHERE cr.ptype = 'g' " +
            "   AND cr.v0    = #{uid}")
    Set<Long> getPermissionIdsByUid(@Param("uid") Long uid);

}
