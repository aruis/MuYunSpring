package net.ximatai.muyun.spring.boot;

import net.ximatai.muyun.database.core.IDatabaseOperations;
import net.ximatai.muyun.database.core.orm.DatabaseValueConverter;
import net.ximatai.muyun.database.quarkus.MuYunJdbiConfigurer;
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
import io.quarkus.arc.DefaultBean;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

import java.math.BigInteger;
import java.sql.Types;

@ApplicationScoped
public class MuYunSpringDatabaseConfiguration {
    @Produces
    @ApplicationScoped
    @DefaultBean
    StaticSchemaService staticSchemaService(IDatabaseOperations operations,
                                            PlatformRuntimeModeProvider runtimeModeProvider) {
        return new StaticSchemaService(operations, new StaticEntityTableMapper(),
                new PlatformSchemaMigrationPolicy(runtimeModeProvider));
    }

    @Produces
    @ApplicationScoped
    MuYunJdbiConfigurer bigIntegerJdbiConfigurer() {
        return jdbi -> jdbi.registerArgument(new AbstractArgumentFactory<BigInteger>(Types.BIGINT) {
            @Override
            protected Argument build(BigInteger value, ConfigRegistry config) {
                return (position, statement, context) -> statement.setLong(position, value.longValueExact());
            }
        });
    }

    @Produces
    @ApplicationScoped
    @DefaultBean
    CodeSequenceAllocator codeSequenceAllocator(Jdbi jdbi) {
        return new PostgresCodeSequenceAllocator(jdbi);
    }

    @Produces
    @ApplicationScoped
    @DefaultBean
    CodeRecycleConsumer codeRecycleConsumer(Jdbi jdbi) {
        return new PostgresCodeRecycleConsumer(jdbi);
    }
}
