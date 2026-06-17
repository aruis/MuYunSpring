package net.ximatai.muyun.spring.platform.runtime;

import net.ximatai.muyun.database.core.orm.MigrationOptions;
import net.ximatai.muyun.spring.dynamic.metadata.ModuleDefinition;
import net.ximatai.muyun.spring.dynamic.refresh.DynamicModuleRefreshResult;
import net.ximatai.muyun.spring.dynamic.refresh.DynamicModuleRuntimeRefresher;
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
    void shouldExposeRefreshFacadeWithoutCreatingSeparateLifecycleConcept() {
        PlatformDynamicRuntimeRefresher refresher = mock(PlatformDynamicRuntimeRefresher.class);
        DynamicModuleRefreshResult refreshed = result(false);
        DynamicModuleRefreshResult preview = result(true);
        when(refresher.refresh("crm.contract")).thenReturn(refreshed);
        when(refresher.previewRefresh("crm.contract")).thenReturn(preview);
        PlatformDynamicRuntimeRefreshService service = new PlatformDynamicRuntimeRefreshService(refresher);

        assertThat(service.refresh("crm.contract")).isSameAs(refreshed);
        assertThat(service.previewRefresh("crm.contract")).isSameAs(preview);

        verify(refresher).refresh("crm.contract");
        verify(refresher).previewRefresh("crm.contract");
    }

    @Test
    void shouldUseExecuteMigrationOptionsForRefresh() {
        PlatformModuleDefinitionCompiler compiler = mock(PlatformModuleDefinitionCompiler.class);
        DynamicModuleRuntimeRefresher dynamicRefresher = mock(DynamicModuleRuntimeRefresher.class);
        when(compiler.compile("crm.contract")).thenReturn(module);
        when(dynamicRefresher.refresh(eq(module), any(MigrationOptions.class))).thenReturn(result(false));
        PlatformDynamicRuntimeRefresher refresher = new PlatformDynamicRuntimeRefresher(compiler, dynamicRefresher);

        DynamicModuleRefreshResult result = refresher.refresh("crm.contract");

        ArgumentCaptor<MigrationOptions> optionsCaptor = ArgumentCaptor.forClass(MigrationOptions.class);
        verify(dynamicRefresher).refresh(eq(module), optionsCaptor.capture());
        assertThat(result.dryRun()).isFalse();
        assertThat(optionsCaptor.getValue().isDryRun()).isFalse();
    }

    @Test
    void shouldUseDryRunMigrationOptionsForPreviewRefresh() {
        PlatformModuleDefinitionCompiler compiler = mock(PlatformModuleDefinitionCompiler.class);
        DynamicModuleRuntimeRefresher dynamicRefresher = mock(DynamicModuleRuntimeRefresher.class);
        when(compiler.compile("crm.contract")).thenReturn(module);
        when(dynamicRefresher.refresh(eq(module), any(MigrationOptions.class))).thenReturn(result(true));
        PlatformDynamicRuntimeRefresher refresher = new PlatformDynamicRuntimeRefresher(compiler, dynamicRefresher);

        DynamicModuleRefreshResult result = refresher.previewRefresh("crm.contract");

        ArgumentCaptor<MigrationOptions> optionsCaptor = ArgumentCaptor.forClass(MigrationOptions.class);
        verify(dynamicRefresher).refresh(eq(module), optionsCaptor.capture());
        assertThat(result.dryRun()).isTrue();
        assertThat(optionsCaptor.getValue().isDryRun()).isTrue();
    }

    private DynamicModuleRefreshResult result(boolean dryRun) {
        return new DynamicModuleRefreshResult(module, Map.of(), dryRun);
    }
}
