package net.ximatai.muyun.spring.boot;

import net.ximatai.muyun.spring.boot.dynamic.DynamicRecordWebController;
import net.ximatai.muyun.spring.boot.platform.StaticModuleDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.ModuleDefinition;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicEntityService;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicRecordRuntime;
import net.ximatai.muyun.spring.iam.user.UserAccountService;
import net.ximatai.muyun.spring.iam.user.UserSessionService;
import net.ximatai.muyun.spring.platform.code.CodeGenerateService;
import net.ximatai.muyun.spring.platform.code.DynamicCodeCoordinator;
import net.ximatai.muyun.spring.platform.workflow.WorkflowAdminService;
import net.ximatai.muyun.spring.platform.workflow.WorkflowTaskActionRequest;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class ConstructorDesignContractTest {
    @Test
    void springComponentsExposeOneProductionConstructor() {
        assertPublicConstructorCount(UserSessionService.class, 1);
        assertPublicConstructorCount(DynamicRecordWebController.class, 1);
        assertPublicConstructorCount(CodeGenerateService.class, 1);
        assertPublicConstructorCount(DynamicCodeCoordinator.class, 1);
        assertPublicConstructorCount(WorkflowAdminService.class, 1);
    }

    @Test
    void runtimeAndDefinitionTypesKeepSmallNamedConstructionSurfaces() {
        assertPublicConstructorCount(DynamicRecordRuntime.class, 1);
        assertPublicConstructorCount(DynamicEntityService.class, 1);
        assertPublicConstructorCount(UserAccountService.class, 2);
        assertPublicConstructorCount(ModuleDefinition.class, 2);
        assertPublicConstructorCount(StaticModuleDefinition.class, 1);
        assertPublicConstructorCount(WorkflowTaskActionRequest.class, 1);
    }

    private void assertPublicConstructorCount(Class<?> type, int expected) {
        long count = Arrays.stream(type.getDeclaredConstructors())
                .map(Constructor::getModifiers)
                .filter(Modifier::isPublic)
                .count();
        assertThat(count)
                .as("public constructors of %s", type.getSimpleName())
                .isEqualTo(expected);
    }
}
