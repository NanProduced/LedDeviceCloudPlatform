package org.nan.cloud.core.infrastructure.repository.mysql.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.nan.cloud.core.infrastructure.repository.mysql.DO.UserDO;

@Mapper
public interface UserMapper extends BaseMapper<UserDO> {

    /**
     * 判断用户 A 是否在 用户 B 的上级组 或 同级组
     *
     * @param aUserId 用户 A 的 ID
     * @param bUserId 用户 B 的 ID
     * @return true 如果 A 是 B 的上级组 或 同级组；否则 false
     */
    @Select({
            "SELECT",
            "  CASE",
            "    WHEN (",
            "      ugB.path LIKE CONCAT(ugA.path, '|', '%')",
            "      OR ugA.parent = ugB.parent",
            "    ) THEN TRUE",
            "    ELSE FALSE",
            "  END",
            "FROM `user` uA",
            "  JOIN user_group ugA ON ugA.ugid = uA.ugid",
            "  JOIN `user` uB ON uB.id = #{bUserId}",
            "  JOIN user_group ugB ON ugB.ugid = uB.ugid",
            "WHERE uA.id = #{aUserId}"
    })
    boolean isAncestorOrSiblingByUser(
            @Param("aUserId") Long aUserId,
            @Param("bUserId") Long bUserId
    );
}
