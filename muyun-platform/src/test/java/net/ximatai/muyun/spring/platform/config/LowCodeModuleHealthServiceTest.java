package net.ximatai.muyun.spring.platform.config;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static net.ximatai.muyun.spring.platform.config.LowCodeConfigTestFixtures.fullPackage;

class LowCodeModuleHealthServiceTest {
    @Test
    void shouldPassWhenNoCheckerReportsIssue() {
        LowCodeModuleHealthService service = new LowCodeModuleHealthService(List.of());

        LowCodeConfigHealthReport report = service.check(new LowCodeModuleHealthContext("crm.contract", null));

        assertThat(report.status()).isEqualTo(LowCodeConfigHealthStatus.PASS);
        assertThat(report.passed()).isTrue();
        assertThat(report.items()).isEmpty();
    }

    @Test
    void shouldIgnoreNullCheckers() {
        LowCodeModuleHealthService service = new LowCodeModuleHealthService(java.util.Arrays.asList(
                null,
                context -> List.of(LowCodeConfigHealthItem.warn(LowCodeConfigHealthScope.PAGE, "PAGE_UNUSED",
                        "page config is not referenced", "uiConfig", "ui-list", null))
        ));

        LowCodeConfigHealthReport report = service.check(new LowCodeModuleHealthContext("crm.contract", null));

        assertThat(report.status()).isEqualTo(LowCodeConfigHealthStatus.WARN);
        assertThat(report.items()).hasSize(1);
    }

    @Test
    void shouldAggregateWarningsAndErrors() {
        LowCodeModuleHealthChecker warningChecker = context -> List.of(LowCodeConfigHealthItem.warn(
                LowCodeConfigHealthScope.PAGE,
                "PAGE_UNUSED",
                "page config is not referenced by menu entry",
                "uiConfig",
                "ui-list",
                "Remove it or bind it to an entry"
        ));
        LowCodeModuleHealthChecker errorChecker = context -> List.of(LowCodeConfigHealthItem.error(
                LowCodeConfigHealthScope.DEPENDENCY,
                "DEPENDENCY_MISSING",
                "dictionary is missing",
                "dictionary",
                "crm.contract_status",
                "Create the dictionary or remove the binding"
        ));
        LowCodeModuleHealthService service = new LowCodeModuleHealthService(List.of(warningChecker, errorChecker));

        LowCodeConfigHealthReport report = service.check(new LowCodeModuleHealthContext("crm.contract", null));

        assertThat(report.status()).isEqualTo(LowCodeConfigHealthStatus.FAIL);
        assertThat(report.items()).extracting(LowCodeConfigHealthItem::code)
                .containsExactly("PAGE_UNUSED", "DEPENDENCY_MISSING");
    }

    @Test
    void shouldWarnWhenOnlyWarningsExist() {
        LowCodeModuleHealthService service = new LowCodeModuleHealthService(List.of(context -> List.of(
                LowCodeConfigHealthItem.warn(LowCodeConfigHealthScope.PAGE, "PAGE_UNUSED",
                        "page config is not referenced", "uiConfig", "ui-list", null)
        )));

        LowCodeConfigHealthReport report = service.check(new LowCodeModuleHealthContext("crm.contract", null));

        assertThat(report.status()).isEqualTo(LowCodeConfigHealthStatus.WARN);
        assertThat(report.passed()).isFalse();
    }

    @Test
    void reportStatusShouldAlwaysBeDerivedFromItems() {
        LowCodeConfigHealthReport report = new LowCodeConfigHealthReport(
                "crm.contract",
                LowCodeConfigHealthStatus.PASS,
                List.of(LowCodeConfigHealthItem.error(LowCodeConfigHealthScope.PAGE, "PAGE_INVALID",
                        "page config is invalid", "uiConfig", "ui-list", null))
        );

        assertThat(report.status()).isEqualTo(LowCodeConfigHealthStatus.FAIL);
        assertThat(report.passed()).isFalse();
    }

    @Test
    void packageCheckerShouldReportInvalidPackageAsHealthFailure() {
        LowCodeModulePackage modulePackage = new LowCodeModulePackage(
                "m10.v1",
                LowCodePackageMode.PAGE_ONLY,
                "crm",
                "crm.contract",
                List.of(LowCodeConfigBundle.included(LowCodePackageBundleType.METADATA,
                        Map.of("module", "crm.contract"))),
                null,
                null
        );
        LowCodeModuleHealthService service = new LowCodeModuleHealthService(
                List.of(new LowCodeModulePackageHealthChecker()));

        LowCodeConfigHealthReport report = service.check(LowCodeModuleHealthContext.ofPackage(modulePackage));

        assertThat(report.status()).isEqualTo(LowCodeConfigHealthStatus.FAIL);
        assertThat(report.items()).hasSize(1);
        assertThat(report.items().getFirst())
                .extracting(LowCodeConfigHealthItem::scope, LowCodeConfigHealthItem::code,
                        LowCodeConfigHealthItem::targetType, LowCodeConfigHealthItem::targetId)
                .containsExactly(LowCodeConfigHealthScope.PACKAGE, "PACKAGE_INVALID", "package", "crm.contract");
    }

    @Test
    void packageCheckerShouldReportContextModuleMismatch() {
        LowCodeModulePackage modulePackage = fullPackage("crm.contract");
        LowCodeModuleHealthService service = new LowCodeModuleHealthService(
                List.of(new LowCodeModulePackageHealthChecker()));

        LowCodeConfigHealthReport report = service.check(new LowCodeModuleHealthContext("crm.customer", modulePackage));

        assertThat(report.status()).isEqualTo(LowCodeConfigHealthStatus.FAIL);
        assertThat(report.items().getFirst().code()).isEqualTo("PACKAGE_MODULE_MISMATCH");
    }

    @Test
    void packageCheckerShouldPassValidPackage() {
        LowCodeModuleHealthService service = new LowCodeModuleHealthService(
                List.of(new LowCodeModulePackageHealthChecker()));

        LowCodeConfigHealthReport report = service.check(LowCodeModuleHealthContext.ofPackage(fullPackage("crm.contract")));

        assertThat(report.status()).isEqualTo(LowCodeConfigHealthStatus.PASS);
        assertThat(report.items()).isEmpty();
    }

    @Test
    void shouldWireHealthServiceWithPackageCheckerInSpringContext() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.register(LowCodeModuleHealthService.class, LowCodeModulePackageHealthChecker.class);
            context.refresh();
            LowCodeModuleHealthService service = context.getBean(LowCodeModuleHealthService.class);

            LowCodeConfigHealthReport report = service.check(LowCodeModuleHealthContext.ofPackage(fullPackage("crm.contract")));

            assertThat(report.status()).isEqualTo(LowCodeConfigHealthStatus.PASS);
        }
    }

    @Test
    void shouldRejectMissingContext() {
        LowCodeModuleHealthService service = new LowCodeModuleHealthService(List.of());

        assertThatThrownBy(() -> service.check(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("health context must not be null");
    }
}
