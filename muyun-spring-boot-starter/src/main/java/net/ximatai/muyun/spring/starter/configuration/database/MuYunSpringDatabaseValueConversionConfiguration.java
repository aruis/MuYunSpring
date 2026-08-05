package net.ximatai.muyun.spring.starter.configuration.database;

import net.ximatai.muyun.database.core.orm.DatabaseValueConverter;
import net.ximatai.muyun.spring.common.schema.PlatformDatabaseValueConverter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** 为静态和动态记录提供一致的数据库字段值转换契约。 */
@Configuration(proxyBeanMethods = false)
public class MuYunSpringDatabaseValueConversionConfiguration {
    @Bean
    @ConditionalOnMissingBean
    /** 默认转换器仅在应用未提供替代实现时生效。 */
    DatabaseValueConverter databaseValueConverter() {
        return new PlatformDatabaseValueConverter();
    }
}
