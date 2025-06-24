package org.nan.cloud.core.casbin;

import org.casbin.adapter.JDBCAdapter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
public class CasbinConfig {

    @Bean
    @Qualifier("rbacAdapter")
    public JDBCAdapter rbacAdapter(DataSource ds) throws Exception {
        return new JDBCAdapter(ds, false, "rbac_casbin_rules", true);
    }

    @Bean
    @Qualifier("abacAdapter")
    public JDBCAdapter abacAdapter(DataSource ds) throws Exception {
        return new JDBCAdapter(ds, false, "abac_casbin_rules", true);
    }


}
