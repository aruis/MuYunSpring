package net.ximatai.muyun.spring.boot.platform;

import net.ximatai.muyun.spring.boot.iam.UserAccountWebController;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StaticModuleDefinitionScannerTest {
    @Test
    void shouldScanStaticModuleAndActionsFromControllerAnnotations() {
        try (GenericApplicationContext context = new GenericApplicationContext()) {
            context.registerBean(UserAccountWebController.class, () -> new UserAccountWebController(null));
            context.refresh();
            StaticModuleDefinitionScanner scanner = new StaticModuleDefinitionScanner(context);

            List<StaticModuleDefinition> definitions = scanner.scan();

            assertThat(definitions).singleElement().satisfies(definition -> {
                assertThat(definition.applicationAlias()).isEqualTo("iam");
                assertThat(definition.moduleAlias()).isEqualTo("iam.user");
                assertThat(definition.title()).isEqualTo("用户管理");
                assertThat(definition.actions()).extracting(StaticModuleActionDefinition::actionCode)
                        .containsExactly("create", "view", "update", "delete", "query",
                                "sort", "enable", "disable", "changePassword");
                assertThat(definition.actions()).filteredOn(action -> action.actionCode().equals("changePassword"))
                        .singleElement()
                        .satisfies(action -> {
                            assertThat(action.title()).isEqualTo("修改密码");
                            assertThat(action.dataAuth()).isTrue();
                        });
            });
        }
    }

    @Test
    void shouldRejectStaticModuleAliasDifferentFromWebScope() {
        try (GenericApplicationContext context = new GenericApplicationContext()) {
            context.registerBean(BadAliasWeb.class);
            context.refresh();
            StaticModuleDefinitionScanner scanner = new StaticModuleDefinitionScanner(context);

            assertThatThrownBy(scanner::scan)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("alias must match web scope");
        }
    }

    @RestController
    @PlatformStaticModule(application = "iam", alias = "iam.bad", title = "Bad")
    @RequestMapping("/iam.good")
    static class BadAliasWeb extends net.ximatai.muyun.spring.boot.web.WebSupport<Object> {
    }
}
