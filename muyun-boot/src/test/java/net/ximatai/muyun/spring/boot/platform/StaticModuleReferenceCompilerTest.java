package net.ximatai.muyun.spring.boot.platform;

import net.ximatai.muyun.spring.ability.reference.ModuleReference;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StaticModuleReferenceCompilerTest {
    @Test
    void shouldCompileStrongReferenceFromStaticModelField() {
        List<StaticModuleReferenceDefinition> references = StaticModuleReferenceCompiler.compile(BindingModel.class);

        assertThat(references)
                .containsExactly(
                        new StaticModuleReferenceDefinition("employee", "employeeId", "iam.employee", "id"),
                        new StaticModuleReferenceDefinition("owner", "ownerUserId", "iam.user", "id")
                );
    }

    @Test
    void shouldRequireExactlyOneTargetDeclaration() {
        assertThatThrownBy(() -> StaticModuleReferenceCompiler.compile(MissingTargetModel.class))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("exactly one of target or targetModuleAlias");

        assertThatThrownBy(() -> StaticModuleReferenceCompiler.compile(DuplicateTargetModel.class))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("exactly one of target or targetModuleAlias");
    }

    @Test
    void shouldRequireTargetServiceModuleAlias() {
        assertThatThrownBy(() -> StaticModuleReferenceCompiler.compile(BadServiceTargetModel.class))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("requires public MODULE_ALIAS");
    }

    @Test
    void shouldKeepStaticReferenceTargetFieldOnPrimaryIdForNow() {
        assertThatThrownBy(() -> StaticModuleReferenceCompiler.compile(NonPrimaryTargetFieldModel.class))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("currently only supports id");
    }

    private static class BindingModel {
        @ModuleReference(targetModuleAlias = "iam.employee")
        private String employeeId;

        @ModuleReference(code = "owner", target = UserService.class)
        private String ownerUserId;
    }

    private static class MissingTargetModel {
        @ModuleReference
        private String employeeId;
    }

    private static class DuplicateTargetModel {
        @ModuleReference(target = UserService.class, targetModuleAlias = "iam.user")
        private String userId;
    }

    private static class BadServiceTargetModel {
        @ModuleReference(target = ServiceWithoutModuleAlias.class)
        private String userId;
    }

    private static class NonPrimaryTargetFieldModel {
        @ModuleReference(code = "user", targetModuleAlias = "iam.user", targetField = "code")
        private String userCode;
    }

    public static class UserService {
        public static final String MODULE_ALIAS = "iam.user";
    }

    public static class ServiceWithoutModuleAlias {
    }
}
