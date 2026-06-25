package net.ximatai.muyun.spring.boot;

import net.ximatai.muyun.database.core.IDatabaseOperations;
import net.ximatai.muyun.database.core.orm.DatabaseValueConverter;
import net.ximatai.muyun.database.core.orm.EntityMetaResolver;
import net.ximatai.muyun.database.core.orm.SimpleEntityManager;
import net.ximatai.muyun.database.spring.boot.JdbiConfigurer;
import net.ximatai.muyun.spring.common.runtime.PlatformRuntimeModeProvider;
import net.ximatai.muyun.spring.common.schema.PlatformEntityManagers;
import net.ximatai.muyun.spring.common.schema.PlatformSchemaMigrationPolicy;
import net.ximatai.muyun.spring.common.schema.StaticEntityTableMapper;
import net.ximatai.muyun.spring.common.schema.StaticSchemaService;
import net.ximatai.muyun.spring.platform.code.CodeRecycleConsumer;
import net.ximatai.muyun.spring.platform.code.CodeSequenceAllocator;
import net.ximatai.muyun.spring.platform.code.PostgresCodeRecycleConsumer;
import net.ximatai.muyun.spring.platform.code.PostgresCodeSequenceAllocator;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.argument.AbstractArgumentFactory;
import org.jdbi.v3.core.argument.Argument;
import org.jdbi.v3.core.config.ConfigRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.math.BigInteger;
import java.sql.Types;

@Configuration(proxyBeanMethods = false)
@Import({MuYunSpringDatabaseValueConversionConfiguration.class, MuYunSpringRuntimeConfiguration.class})
public class MuYunSpringDatabaseConfiguration {
    @Bean
    @ConditionalOnMissingBean
    EntityMetaResolver entityMetaResolver() {
        return PlatformEntityManagers.entityMetaResolver();
    }

    @Bean
    @ConditionalOnMissingBean
    SimpleEntityManager simpleEntityManager(IDatabaseOperations<?> operations,
                                            EntityMetaResolver entityMetaResolver,
                                            DatabaseValueConverter databaseValueConverter) {
        return PlatformEntityManagers.simpleEntityManager(operations, entityMetaResolver, databaseValueConverter);
    }

    @Bean
    @ConditionalOnBean(IDatabaseOperations.class)
    @ConditionalOnMissingBean
    StaticSchemaService staticSchemaService(IDatabaseOperations<?> operations,
                                            PlatformRuntimeModeProvider runtimeModeProvider) {
        return new StaticSchemaService(operations, new StaticEntityTableMapper(),
                new PlatformSchemaMigrationPolicy(runtimeModeProvider));
    }

    @Bean
    JdbiConfigurer bigIntegerJdbiConfigurer() {
        return jdbi -> jdbi.registerArgument(new AbstractArgumentFactory<BigInteger>(Types.BIGINT) {
            @Override
            protected Argument build(BigInteger value, ConfigRegistry config) {
                return (position, statement, context) -> statement.setLong(position, value.longValueExact());
            }
        });
    }

    @Bean
    @ConditionalOnBean(Jdbi.class)
    @ConditionalOnMissingBean(CodeSequenceAllocator.class)
    CodeSequenceAllocator codeSequenceAllocator(Jdbi jdbi) {
        return new PostgresCodeSequenceAllocator(jdbi);
    }

    @Bean
    @ConditionalOnBean(Jdbi.class)
    @ConditionalOnMissingBean(CodeRecycleConsumer.class)
    CodeRecycleConsumer codeRecycleConsumer(Jdbi jdbi) {
        return new PostgresCodeRecycleConsumer(jdbi);
    }
}
