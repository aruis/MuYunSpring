package net.ximatai.muyun.spring.boot;

import net.ximatai.muyun.database.core.IDatabaseOperations;
import net.ximatai.muyun.database.core.orm.EntityMetaResolver;
import net.ximatai.muyun.database.core.orm.SimpleEntityManager;
import net.ximatai.muyun.spring.common.schema.PlatformEntityManagers;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class MuYunSpringDatabaseConfiguration {
    @Bean
    @ConditionalOnMissingBean
    EntityMetaResolver entityMetaResolver() {
        return PlatformEntityManagers.entityMetaResolver();
    }

    @Bean
    @ConditionalOnMissingBean
    SimpleEntityManager simpleEntityManager(IDatabaseOperations<?> operations, EntityMetaResolver entityMetaResolver) {
        return PlatformEntityManagers.simpleEntityManager(operations, entityMetaResolver);
    }
}
