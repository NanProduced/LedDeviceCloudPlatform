package org.nan.cloud.core.infrastructure.repository.mysql.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.nan.cloud.core.infrastructure.repository.mysql.DO.UserGroupDO;

@Mapper
public interface UserGroupMapper extends BaseMapper<UserGroupDO> {

    /**
     * 判断 groupA (ugid = aUgid) 是否是 groupB (ugid = bUgid) 的上级组
     * @return true: 是上级组；false: 不是上级组
     */
    @Select(
            "SELECT " +
                    "  CASE " +
                    "    WHEN B.path LIKE CONCAT(A.path, '|', '%') THEN TRUE " +
                    "    ELSE FALSE " +
                    "  END " +
                    "FROM user_group A, user_group B " +
                    "WHERE A.ugid = #{aUgid} " +
                    "  AND B.ugid = #{bUgid}"
    )
    boolean isAncestor(@Param("aUgid") Long aUgid, @Param("bUgid") Long bUgid);

    /**
     * 判断 groupA (aUgid) 是否是 groupB (bUgid) 的上级组或同级组
     * @return true: 是上级组或同级组；false: 否
     */
    @Select({
            "SELECT",
            "  CASE",
            "    WHEN (",
            "      B.path LIKE CONCAT(A.path, '|', '%')",
            "      OR A.parent = B.parent",
            "    ) THEN TRUE",
            "    ELSE FALSE",
            "  END",
            "FROM user_group A",
            "JOIN user_group B",
            "  ON A.ugid = #{aUgid}",
            " AND B.ugid = #{bUgid}"
    })
    boolean isAncestorOrSibling(@Param("aUgid") Long aUgid, @Param("bUgid") Long bUgid);

    /**
     * 判断 groupA (aUgid) 是否是 groupB (bUgid) 的上级组
     * @return true: 是上级组；false: 否
     */
    @Select(
            "SELECT " +
                    "  CASE " +
                    "    WHEN A.parent = B.parent) THEN TRUE " +
                    "    ELSE FALSE " +
                    "  END " +
                    "FROM user_group A, user_group B " +
                    "WHERE A.ugid = #{aUgid} " +
                    "  AND B.ugid = #{bUgid}"
    )
    boolean isSibling(@Param("aUgid") Long aUgid, @Param("bUgid") Long bUgid);
}
