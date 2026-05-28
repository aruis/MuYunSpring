package net.ximatai.muyun.spring.platform.publish;

import net.ximatai.muyun.database.core.orm.MigrationOptions;
import net.ximatai.muyun.spring.dynamic.metadata.ModuleDefinition;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicModulePublishResult;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicModulePublisher;
import org.springframework.stereotype.Service;

@Service
public class PlatformDynamicModulePublisher {
    private final PlatformModuleDefinitionCompiler compiler;
    private final DynamicModulePublisher publisher;

    public PlatformDynamicModulePublisher(PlatformModuleDefinitionCompiler compiler, DynamicModulePublisher publisher) {
        this.compiler = compiler;
        this.publisher = publisher;
    }

    public DynamicModulePublishResult publish(String moduleAlias) {
        return publish(moduleAlias, MigrationOptions.execute());
    }

    public DynamicModulePublishResult preview(String moduleAlias) {
        return publish(moduleAlias, MigrationOptions.dryRun());
    }

    public DynamicModulePublishResult publish(String moduleAlias, MigrationOptions options) {
        ModuleDefinition definition = compiler.compile(moduleAlias);
        return publisher.publish(definition, options);
    }
}
