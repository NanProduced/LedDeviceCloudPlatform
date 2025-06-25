package org.nan.cloud.core.infrastructure.repository.mysql;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan("org.nan.cloud.core.infrastructure.repository.mysql.mapper")
public class MyBatisConfig {
}
