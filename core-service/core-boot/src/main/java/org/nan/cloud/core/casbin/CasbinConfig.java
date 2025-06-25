package org.nan.cloud.core.casbin;

import org.casbin.adapter.JDBCAdapter;
import org.casbin.jcasbin.main.Enforcer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
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

    @Bean
    @Qualifier("rbacEnforcer")
    public Enforcer rbacEnforcer(@Value("classpath:casbin/model_rbac.conf") String modelPath,
                                 @Qualifier("rbacAdapter") JDBCAdapter adapter) {
        Enforcer e = new Enforcer(modelPath, adapter);
        e.loadPolicy();
        return e;
    }

}
