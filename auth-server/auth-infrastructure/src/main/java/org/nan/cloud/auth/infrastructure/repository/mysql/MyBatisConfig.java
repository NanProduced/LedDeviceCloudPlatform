package org.nan.cloud.auth.infrastructure.repository.mysql;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan("org.nan.auth.infrastructure.repository.mysql.mapper")
public class MyBatisConfig {
}
