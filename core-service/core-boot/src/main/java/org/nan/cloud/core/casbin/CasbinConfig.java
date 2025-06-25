package org.nan.cloud.core.casbin;

import org.casbin.adapter.JDBCAdapter;
import org.casbin.jcasbin.main.Enforcer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import javax.sql.DataSource;
import java.io.IOException;

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
    public Enforcer rbacEnforcer(ResourceLoader resourceLoader,
                                 @Qualifier("rbacAdapter") JDBCAdapter adapter) throws IOException {
        Resource resource = resourceLoader.getResource("classpath:casbin/model_rbac.conf");
        String modelPath = resource.getFile().getAbsolutePath(); // 转成文件绝对路径
        Enforcer enforcer = new Enforcer(modelPath, adapter);
        enforcer.loadPolicy();
        return enforcer;
    }

}
