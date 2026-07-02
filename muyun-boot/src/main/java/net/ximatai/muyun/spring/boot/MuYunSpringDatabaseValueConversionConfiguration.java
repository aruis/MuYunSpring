package net.ximatai.muyun.spring.boot;

import net.ximatai.muyun.database.core.orm.DatabaseValueConverter;
import net.ximatai.muyun.spring.common.schema.PlatformDatabaseValueConverter;
import io.quarkus.arc.DefaultBean;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

@ApplicationScoped
public class MuYunSpringDatabaseValueConversionConfiguration {
    @Produces
    @ApplicationScoped
    @DefaultBean
    DatabaseValueConverter databaseValueConverter() {
        return new PlatformDatabaseValueConverter();
    }
}
