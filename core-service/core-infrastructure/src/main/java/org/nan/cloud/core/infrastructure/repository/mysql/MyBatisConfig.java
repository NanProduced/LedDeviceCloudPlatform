package org.nan.cloud.core.infrastructure.repository.mysql;

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan("org.nan.cloud.core.infrastructure.repository.mysql.mapper")
public class MyBatisConfig {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        // 最新版推荐用 PaginationInnerInterceptor
//        interceptor.addInnerInterceptor();
        return interceptor;
    }
}
