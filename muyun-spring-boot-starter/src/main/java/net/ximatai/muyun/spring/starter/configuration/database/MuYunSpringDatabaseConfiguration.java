package net.ximatai.muyun.spring.starter.configuration.database;

import net.ximatai.muyun.spring.starter.configuration.runtime.MuYunSpringRuntimeConfiguration;

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
import org.springframework.context.annotation.Primary;

import java.math.BigInteger;
import java.sql.Types;

/**
 * 数据库适配装配：把 MuYunDatabase 的实体解析、Schema 治理及 PostgreSQL 专属实现
 * 接入 Spring，不把这些基础设施细节泄漏到领域 Service。
 */
@Configuration(proxyBeanMethods = false)
@Import({MuYunSpringDatabaseValueConversionConfiguration.class, MuYunSpringRuntimeConfiguration.class})
public class MuYunSpringDatabaseConfiguration {
    @Bean
    @Primary
    @ConditionalOnMissingBean
    /** 提供静态模型与动态模型共用的实体元数据解析器。 */
    EntityMetaResolver platformEntityMetaResolver() {
        return PlatformEntityManagers.entityMetaResolver();
    }

    @Bean
    @Primary
    @ConditionalOnMissingBean
    /** 将数据库操作、实体元数据和字段转换组合为统一实体管理器。 */
    SimpleEntityManager platformSimpleEntityManager(IDatabaseOperations<?> operations,
                                                    EntityMetaResolver entityMetaResolver,
                                                    DatabaseValueConverter databaseValueConverter) {
        return PlatformEntityManagers.simpleEntityManager(operations, entityMetaResolver, databaseValueConverter);
    }

    @Bean
    @ConditionalOnBean(IDatabaseOperations.class)
    @ConditionalOnMissingBean
    /** 按运行模式提供静态 Schema 的默认治理策略。 */
    StaticSchemaService staticSchemaService(IDatabaseOperations<?> operations,
                                            PlatformRuntimeModeProvider runtimeModeProvider) {
        return new StaticSchemaService(operations, new StaticEntityTableMapper(),
                new PlatformSchemaMigrationPolicy(runtimeModeProvider));
    }

    @Bean
    /** 统一 BigInteger 写入参数，避免 PostgreSQL/JDBC 的隐式窄化。 */
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
    /** 在 PostgreSQL 环境启用编码规则的序列分配器。 */
    CodeSequenceAllocator codeSequenceAllocator(Jdbi jdbi) {
        return new PostgresCodeSequenceAllocator(jdbi);
    }

    @Bean
    @ConditionalOnBean(Jdbi.class)
    @ConditionalOnMissingBean(CodeRecycleConsumer.class)
    /** 在 PostgreSQL 环境启用编码回收池消费实现。 */
    CodeRecycleConsumer codeRecycleConsumer(Jdbi jdbi) {
        return new PostgresCodeRecycleConsumer(jdbi);
    }
}
