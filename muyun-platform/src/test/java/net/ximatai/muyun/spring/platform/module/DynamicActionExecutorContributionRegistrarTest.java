package net.ximatai.muyun.spring.platform.module;

import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicActionExecutionContext;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicActionExecutionRequest;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicActionExecutor;
import net.ximatai.muyun.spring.platform.support.TestMemoryDao;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DynamicActionExecutorContributionRegistrarTest {
    @Test
    void shouldRejectContributionToStaticModule() {
        TestMemoryDao<PlatformModule> moduleDao = new TestMemoryDao<>();
        PlatformModuleService moduleService = new PlatformModuleService(moduleDao);
        PlatformModule module = new PlatformModule();
        module.setAlias("sales.contract");
        module.setApplicationAlias("sales");
        module.setTitle("合同");
        module.setModuleKind(ModuleKind.STATIC);
        moduleService.insert(module);
        PlatformModuleActionService actionService = new PlatformModuleActionService(new TestMemoryDao<>(), moduleService);
        ApplicationContext applicationContext = mock(ApplicationContext.class);
        when(applicationContext.getBeansOfType(DynamicActionExecutor.class))
                .thenReturn(Map.of("testExecutor", new TestExecutor()));
        DynamicActionExecutorContributionRegistrar registrar = new DynamicActionExecutorContributionRegistrar(
                applicationContext, new ModuleActionContributionRegistrar(actionService), moduleService);

        assertThatThrownBy(registrar::run)
                .isInstanceOf(PlatformException.class)
                .hasMessageContaining("requires a dynamic module");
    }

    @PlatformDynamicActionContribution(moduleAlias = "sales.contract", actionCode = "sync", title = "同步")
    private static final class TestExecutor implements DynamicActionExecutor {
        @Override
        public String executorKey() {
            return "test.executor";
        }

        @Override
        public Object execute(DynamicActionExecutionContext context, DynamicActionExecutionRequest request) {
            return null;
        }
    }
}
