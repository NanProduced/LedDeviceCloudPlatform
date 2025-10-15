package org.nan.cloud.file.infrastructure.repository.mysql.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.nan.cloud.file.infrastructure.repository.mysql.DO.OrgQuotaDO;

@Mapper
public interface OrgQuotaMapper extends BaseMapper<OrgQuotaDO> {
}