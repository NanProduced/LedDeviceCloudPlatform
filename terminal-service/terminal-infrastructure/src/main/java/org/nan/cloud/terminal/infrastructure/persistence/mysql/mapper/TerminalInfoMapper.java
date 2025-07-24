package org.nan.cloud.terminal.infrastructure.persistence.mysql.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.nan.cloud.terminal.infrastructure.persistence.mysql.entity.TerminalInfoDO;

@Mapper
public interface TerminalInfoMapper extends BaseMapper<TerminalInfoDO> {
}
