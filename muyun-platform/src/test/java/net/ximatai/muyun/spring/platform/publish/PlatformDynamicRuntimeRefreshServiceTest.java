package net.ximatai.muyun.spring.platform.publish;

import net.ximatai.muyun.database.core.orm.MigrationOptions;
import net.ximatai.muyun.spring.dynamic.metadata.ModuleDefinition;
import net.ximatai.muyun.spring.dynamic.publish.DynamicModulePublishResult;
import net.ximatai.muyun.spring.dynamic.publish.DynamicModulePublisher;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PlatformDynamicRuntimeRefreshServiceTest {
    private final ModuleDefinition module = new ModuleDefinition("crm.contract", "Contract", List.of());

    @Test
    void shouldExposeRefreshFacadeWithoutCreatingNewPublishConcept() {
        PlatformDynamicModulePublisher publisher = mock(PlatformDynamicModulePublisher.class);
        DynamicModulePublishResult refreshed = result(false);
        DynamicModulePublishResult preview = result(true);
        when(publisher.publish("crm.contract")).thenReturn(refreshed);
        when(publisher.preview("crm.contract")).thenReturn(preview);
        PlatformDynamicRuntimeRefreshService service = new PlatformDynamicRuntimeRefreshService(publisher);

        assertThat(service.refresh("crm.contract")).isSameAs(refreshed);
        assertThat(service.previewRefresh("crm.contract")).isSameAs(preview);

        verify(publisher).publish("crm.contract");
        verify(publisher).preview("crm.contract");
    }

    @Test
    void shouldUseExecuteMigrationOptionsForRefresh() {
        PlatformModuleDefinitionCompiler compiler = mock(PlatformModuleDefinitionCompiler.class);
        DynamicModulePublisher dynamicPublisher = mock(DynamicModulePublisher.class);
        when(compiler.compile("crm.contract")).thenReturn(module);
        when(dynamicPublisher.publish(eq(module), any(MigrationOptions.class))).thenReturn(result(false));
        PlatformDynamicModulePublisher publisher = new PlatformDynamicModulePublisher(compiler, dynamicPublisher);

        DynamicModulePublishResult result = publisher.publish("crm.contract");

        ArgumentCaptor<MigrationOptions> optionsCaptor = ArgumentCaptor.forClass(MigrationOptions.class);
        verify(dynamicPublisher).publish(eq(module), optionsCaptor.capture());
        assertThat(result.dryRun()).isFalse();
        assertThat(optionsCaptor.getValue().isDryRun()).isFalse();
    }

    @Test
    void shouldUseDryRunMigrationOptionsForPreviewRefresh() {
        PlatformModuleDefinitionCompiler compiler = mock(PlatformModuleDefinitionCompiler.class);
        DynamicModulePublisher dynamicPublisher = mock(DynamicModulePublisher.class);
        when(compiler.compile("crm.contract")).thenReturn(module);
        when(dynamicPublisher.publish(eq(module), any(MigrationOptions.class))).thenReturn(result(true));
        PlatformDynamicModulePublisher publisher = new PlatformDynamicModulePublisher(compiler, dynamicPublisher);

        DynamicModulePublishResult result = publisher.preview("crm.contract");

        ArgumentCaptor<MigrationOptions> optionsCaptor = ArgumentCaptor.forClass(MigrationOptions.class);
        verify(dynamicPublisher).publish(eq(module), optionsCaptor.capture());
        assertThat(result.dryRun()).isTrue();
        assertThat(optionsCaptor.getValue().isDryRun()).isTrue();
    }

    private DynamicModulePublishResult result(boolean dryRun) {
        return new DynamicModulePublishResult(module, Map.of(), dryRun);
    }
}
