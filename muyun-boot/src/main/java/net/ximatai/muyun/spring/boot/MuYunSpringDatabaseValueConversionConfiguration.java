package net.ximatai.muyun.spring.boot;

import net.ximatai.muyun.database.core.orm.DatabaseValueConverter;
import net.ximatai.muyun.spring.common.schema.PlatformDatabaseValueConverter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class MuYunSpringDatabaseValueConversionConfiguration {
    @Bean
    @ConditionalOnMissingBean
    DatabaseValueConverter databaseValueConverter() {
        return new PlatformDatabaseValueConverter();
    }
}
