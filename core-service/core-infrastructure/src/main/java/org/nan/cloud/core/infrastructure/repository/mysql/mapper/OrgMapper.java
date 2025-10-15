package org.nan.cloud.core.infrastructure.repository.mysql.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.nan.cloud.core.infrastructure.repository.mysql.DO.OrganizationDO;

@Mapper
public interface OrgMapper extends BaseMapper<OrganizationDO> {
}
