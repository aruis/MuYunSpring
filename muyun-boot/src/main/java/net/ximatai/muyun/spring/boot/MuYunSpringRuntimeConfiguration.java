package net.ximatai.muyun.spring.boot;

import net.ximatai.muyun.spring.common.runtime.PlatformRuntimeModeProvider;
import io.quarkus.arc.DefaultBean;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

@ApplicationScoped
public class MuYunSpringRuntimeConfiguration {
    @Produces
    @ApplicationScoped
    @DefaultBean
    PlatformRuntimeModeProvider platformRuntimeModeProvider(MuYunSpringRuntimeProperties properties) {
        return new PropertiesPlatformRuntimeModeProvider(properties);
    }
}
