package net.ximatai.muyun.spring.platform.config;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static net.ximatai.muyun.spring.platform.config.LowCodeConfigTestFixtures.fullPackage;
import static net.ximatai.muyun.spring.platform.config.LowCodeConfigTestFixtures.fullPackageWithPageBundle;

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
    void bundleIdentityCheckerShouldReportTopLevelModuleMismatch() {
        LowCodeModulePackage modulePackage = new LowCodeModulePackage(
                "m10.v1",
                LowCodePackageMode.MODULE_FULL,
                "crm",
                "crm.contract",
                List.of(
                        LowCodeConfigBundle.included(LowCodePackageBundleType.METADATA,
                                Map.of("module", "crm.contract")),
                        LowCodeConfigBundle.included(LowCodePackageBundleType.PAGE,
                                Map.of("moduleAlias", "crm.customer", "uiConfigs", List.of("list")))
                ),
                null,
                null
        );
        LowCodeModuleHealthService service = new LowCodeModuleHealthService(
                List.of(new LowCodeModulePackageHealthChecker(), new LowCodeModuleBundleIdentityHealthChecker()));

        LowCodeConfigHealthReport report = service.check(LowCodeModuleHealthContext.ofPackage(modulePackage));

        assertThat(report.status()).isEqualTo(LowCodeConfigHealthStatus.FAIL);
        assertThat(report.items()).extracting(LowCodeConfigHealthItem::scope, LowCodeConfigHealthItem::code,
                        LowCodeConfigHealthItem::targetId)
                .containsExactly(tuple(LowCodeConfigHealthScope.PAGE, "BUNDLE_MODULE_IDENTITY_MISMATCH", "PAGE"));
    }

    @Test
    void dependencyCheckerShouldFailDefaultResolvedDependencyWithoutResolver() {
        LowCodeModulePackage modulePackage = fullPackage("crm.contract",
                List.of(LowCodePackageDependency.action("crm.contract", "submit")));
        LowCodeModuleHealthService service = new LowCodeModuleHealthService(
                List.of(new LowCodeModulePackageHealthChecker(), new LowCodeModuleDependencyHealthChecker()));

        LowCodeConfigHealthReport report = service.check(LowCodeModuleHealthContext.ofPackage(modulePackage));

        assertThat(report.status()).isEqualTo(LowCodeConfigHealthStatus.FAIL);
        assertThat(report.items()).extracting(LowCodeConfigHealthItem::scope, LowCodeConfigHealthItem::code,
                        LowCodeConfigHealthItem::targetType, LowCodeConfigHealthItem::targetId)
                .containsExactly(tuple(LowCodeConfigHealthScope.DEPENDENCY, "DEPENDENCY_RESOLVER_MISSING",
                        "ACTION", "crm.contract:submit"));
    }

    @Test
    void dependencyCheckerShouldWarnManifestOnlyDependencyWithoutResolver() {
        LowCodeModulePackage modulePackage = fullPackage("crm.contract",
                List.of(new LowCodePackageDependency(LowCodePackageDependencyType.WORKFLOW,
                        null, null, "contract_approval", true)));
        LowCodeModuleHealthService service = new LowCodeModuleHealthService(
                List.of(new LowCodeModulePackageHealthChecker(), new LowCodeModuleDependencyHealthChecker()));

        LowCodeConfigHealthReport report = service.check(LowCodeModuleHealthContext.ofPackage(modulePackage));

        assertThat(report.status()).isEqualTo(LowCodeConfigHealthStatus.WARN);
        assertThat(report.items().getFirst())
                .extracting(LowCodeConfigHealthItem::scope, LowCodeConfigHealthItem::code,
                        LowCodeConfigHealthItem::severity, LowCodeConfigHealthItem::targetType)
                .containsExactly(LowCodeConfigHealthScope.DEPENDENCY, "DEPENDENCY_RESOLVER_MISSING",
                        LowCodeConfigHealthSeverity.WARN, "WORKFLOW");
    }

    @Test
    void dependencyCheckerShouldUseExplicitResolverResult() {
        LowCodeModulePackage modulePackage = fullPackage("crm.contract",
                List.of(LowCodePackageDependency.dictionary("crm", "contract_status")));
        LowCodeModuleHealthService service = new LowCodeModuleHealthService(
                List.of(new LowCodeModuleDependencyHealthChecker(List.of(new LowCodeConfigTestFixtures.RecordingDependencyResolver(
                        Set.of(LowCodePackageDependencyType.DICTIONARY), Set.of()
                )))));

        LowCodeConfigHealthReport report = service.check(LowCodeModuleHealthContext.ofPackage(modulePackage));

        assertThat(report.status()).isEqualTo(LowCodeConfigHealthStatus.FAIL);
        assertThat(report.items().getFirst().code()).isEqualTo("REQUIRED_DEPENDENCY_MISSING");
        assertThat(report.items().getFirst().targetId()).isEqualTo("crm:contract_status");
    }

    @Test
    void measureUnitCheckerShouldWarnWhenCategoryDependencyIsMissing() {
        LowCodeModulePackage modulePackage = measureUnitPackage(List.of());
        LowCodeModuleHealthService service = new LowCodeModuleHealthService(
                List.of(new LowCodeMeasureUnitHealthChecker()));

        LowCodeConfigHealthReport report = service.check(LowCodeModuleHealthContext.ofPackage(modulePackage));

        assertThat(report.status()).isEqualTo(LowCodeConfigHealthStatus.WARN);
        assertThat(report.items()).extracting(LowCodeConfigHealthItem::scope, LowCodeConfigHealthItem::code,
                        LowCodeConfigHealthItem::targetType, LowCodeConfigHealthItem::targetId)
                .containsExactly(tuple(LowCodeConfigHealthScope.DEPENDENCY, "MEASURE_UNIT_DEPENDENCY_MISSING",
                        "measureUnit", "quantity"));
    }

    @Test
    void measureUnitCheckerShouldPassWhenMetadataFieldsAndDependencyAreComplete() {
        LowCodeModulePackage modulePackage = measureUnitPackage(
                List.of(LowCodePackageDependency.sharedMeasureUnit("quantity")));
        LowCodeModuleHealthService service = new LowCodeModuleHealthService(
                List.of(new LowCodeMeasureUnitHealthChecker()));

        LowCodeConfigHealthReport report = service.check(LowCodeModuleHealthContext.ofPackage(modulePackage));

        assertThat(report.status()).isEqualTo(LowCodeConfigHealthStatus.PASS);
        assertThat(report.items()).isEmpty();
    }

    @Test
    void moneyCheckerShouldPassWhenMetadataFieldsAreComplete() {
        LowCodeModulePackage modulePackage = moneyPackage(moneyDependencies());
        LowCodeModuleHealthService service = new LowCodeModuleHealthService(
                List.of(new LowCodeMoneyHealthChecker()));

        LowCodeConfigHealthReport report = service.check(LowCodeModuleHealthContext.ofPackage(modulePackage));

        assertThat(report.status()).isEqualTo(LowCodeConfigHealthStatus.PASS);
        assertThat(report.items()).isEmpty();
    }

    @Test
    void moneyCheckerShouldWarnWhenCurrencyOrRateTypeDependencyIsMissing() {
        LowCodeModulePackage modulePackage = moneyPackage(List.of());
        LowCodeModuleHealthService service = new LowCodeModuleHealthService(
                List.of(new LowCodeMoneyHealthChecker()));

        LowCodeConfigHealthReport report = service.check(LowCodeModuleHealthContext.ofPackage(modulePackage));

        assertThat(report.status()).isEqualTo(LowCodeConfigHealthStatus.WARN);
        assertThat(report.items()).extracting(LowCodeConfigHealthItem::scope, LowCodeConfigHealthItem::code,
                        LowCodeConfigHealthItem::targetType, LowCodeConfigHealthItem::targetId)
                .containsExactlyInAnyOrder(
                        tuple(LowCodeConfigHealthScope.DEPENDENCY, "MONEY_CURRENCY_DEPENDENCY_MISSING",
                                "currency", "USD"),
                        tuple(LowCodeConfigHealthScope.DEPENDENCY, "MONEY_CURRENCY_DEPENDENCY_MISSING",
                                "currency", "CNY"),
                        tuple(LowCodeConfigHealthScope.DEPENDENCY, "MONEY_RATE_TYPE_DEPENDENCY_MISSING",
                                "exchangeRateType", "SPOT")
                );
    }

    @Test
    void moneyCheckerShouldSupportRuntimeNestedMoneyContract() {
        LowCodeModulePackage modulePackage = new LowCodeModulePackage(
                "m10.v1",
                LowCodePackageMode.MODULE_FULL,
                "crm",
                "crm.contract",
                List.of(LowCodeConfigBundle.included(LowCodePackageBundleType.METADATA,
                        Map.of(
                                "module", "crm.contract",
                                "fields", List.of(
                                        Map.of(
                                                "fieldName", "amount",
                                                "type", "DECIMAL",
                                                "money", Map.of(
                                                        "currencyMode", "SELECTABLE",
                                                        "defaultCurrencyCode", "USD",
                                                        "currencyFieldName", "amountCurrency",
                                                        "baseAmountFieldName", "amountBase",
                                                        "baseCurrencyCode", "CNY",
                                                        "rateTypeCode", "SPOT",
                                                        "rateDateFieldName", "orderDate",
                                                        "exchangeRateFieldName", "amountExchangeRate"
                                                )
                                        ),
                                        Map.of("fieldName", "amountCurrency", "type", "STRING"),
                                        Map.of("fieldName", "amountBase", "type", "DECIMAL"),
                                        Map.of("fieldName", "orderDate", "type", "DATE"),
                                        Map.of("fieldName", "amountExchangeRate", "type", "DECIMAL")
                                )
                        ))),
                new LowCodePackageDependencyManifest(moneyDependencies()),
                null
        );
        LowCodeModuleHealthService service = new LowCodeModuleHealthService(
                List.of(new LowCodeMoneyHealthChecker()));

        LowCodeConfigHealthReport report = service.check(LowCodeModuleHealthContext.ofPackage(modulePackage));

        assertThat(report.status()).isEqualTo(LowCodeConfigHealthStatus.PASS);
    }

    @Test
    void moneyCheckerShouldIgnoreEmptyRuntimeNestedMoneyContract() {
        LowCodeModulePackage modulePackage = new LowCodeModulePackage(
                "m10.v1",
                LowCodePackageMode.MODULE_FULL,
                "crm",
                "crm.contract",
                List.of(LowCodeConfigBundle.included(LowCodePackageBundleType.METADATA,
                        Map.of(
                                "module", "crm.contract",
                                "fields", List.of(
                                        Map.of(
                                                "fieldName", "title",
                                                "type", "STRING",
                                                "money", Map.of(
                                                        "currencyMode", "",
                                                        "baseAmountFieldName", "",
                                                        "rateTypeCode", ""
                                                )
                                        )
                                )
                        ))),
                new LowCodePackageDependencyManifest(List.of()),
                null
        );
        LowCodeModuleHealthService service = new LowCodeModuleHealthService(
                List.of(new LowCodeMoneyHealthChecker()));

        LowCodeConfigHealthReport report = service.check(LowCodeModuleHealthContext.ofPackage(modulePackage));

        assertThat(report.status()).isEqualTo(LowCodeConfigHealthStatus.PASS);
        assertThat(report.items()).isEmpty();
    }

    @Test
    void moneyCheckerShouldNotTreatFieldTypeAliasAsRuntimeFieldType() {
        LowCodeModulePackage modulePackage = new LowCodeModulePackage(
                "m10.v1",
                LowCodePackageMode.MODULE_FULL,
                "crm",
                "crm.contract",
                List.of(LowCodeConfigBundle.included(LowCodePackageBundleType.METADATA,
                        Map.of(
                                "module", "crm.contract",
                                "fields", List.of(
                                        Map.of(
                                                "fieldName", "amount",
                                                "fieldTypeAlias", "decimal",
                                                "currencyMode", "SELECTABLE",
                                                "currencyFieldName", "amountCurrency",
                                                "baseAmountFieldName", "amountBase",
                                                "rateTypeCode", "SPOT",
                                                "rateDateFieldName", "rateAt"
                                        ),
                                        Map.of("fieldName", "amountCurrency", "fieldTypeAlias", "string"),
                                        Map.of("fieldName", "amountBase", "fieldTypeAlias", "decimal"),
                                        Map.of("fieldName", "rateAt", "fieldTypeAlias", "zoned_datetime")
                                )
                        ))),
                new LowCodePackageDependencyManifest(List.of(LowCodePackageDependency.exchangeRateType("SPOT"))),
                null
        );
        LowCodeModuleHealthService service = new LowCodeModuleHealthService(
                List.of(new LowCodeMoneyHealthChecker()));

        LowCodeConfigHealthReport report = service.check(LowCodeModuleHealthContext.ofPackage(modulePackage));

        assertThat(report.status()).isEqualTo(LowCodeConfigHealthStatus.PASS);
        assertThat(report.items()).isEmpty();
    }

    @Test
    void moneyCheckerShouldResolveConfigurationFieldIds() {
        LowCodeModulePackage modulePackage = new LowCodeModulePackage(
                "m10.v1",
                LowCodePackageMode.MODULE_FULL,
                "crm",
                "crm.contract",
                List.of(LowCodeConfigBundle.included(LowCodePackageBundleType.METADATA,
                        Map.of(
                                "module", "crm.contract",
                                "metadataFields", List.of(
                                        Map.of("id", "f_amount", "fieldName", "amount", "type", "DECIMAL"),
                                        Map.of("id", "f_currency", "fieldName", "amountCurrency", "type", "STRING"),
                                        Map.of("id", "f_base", "fieldName", "amountBase", "type", "DECIMAL"),
                                        Map.of("id", "f_date", "fieldName", "orderDate", "type", "DATE"),
                                        Map.of("id", "f_rate", "fieldName", "amountExchangeRate", "type", "DECIMAL")
                                ),
                                "moduleFields", List.of(
                                        Map.of(
                                                "metadataFieldId", "f_amount",
                                                "moneyCurrencyMode", "SELECTABLE",
                                                "moneyDefaultCurrencyCode", "USD",
                                                "moneyCurrencyFieldId", "f_currency",
                                                "moneyBaseAmountFieldId", "f_base",
                                                "moneyBaseCurrencyCode", "CNY",
                                                "moneyRateTypeCode", "SPOT",
                                                "moneyRateDateFieldId", "f_date",
                                                "moneyExchangeRateFieldId", "f_rate"
                                        )
                                )
                        ))),
                new LowCodePackageDependencyManifest(moneyDependencies()),
                null
        );
        LowCodeModuleHealthService service = new LowCodeModuleHealthService(
                List.of(new LowCodeMoneyHealthChecker()));

        LowCodeConfigHealthReport report = service.check(LowCodeModuleHealthContext.ofPackage(modulePackage));

        assertThat(report.status()).isEqualTo(LowCodeConfigHealthStatus.PASS);
    }

    @Test
    void moneyCheckerShouldReportBrokenMetadataFieldContract() {
        LowCodeModulePackage modulePackage = new LowCodeModulePackage(
                "m10.v1",
                LowCodePackageMode.MODULE_FULL,
                "crm",
                "crm.contract",
                List.of(LowCodeConfigBundle.included(LowCodePackageBundleType.METADATA,
                        Map.of(
                                "module", "crm.contract",
                                "fields", List.of(
                                        Map.of(
                                                "fieldName", "amount",
                                                "type", "DECIMAL",
                                                "currencyMode", "SELECTABLE",
                                                "currencyFieldName", "amountCurrency",
                                                "baseAmountFieldName", "amount",
                                                "baseCurrencyCode", "cn",
                                                "rateTypeCode", "1spot",
                                                "rateDateFieldName", "orderDate",
                                                "exchangeRateFieldName", "amountExchangeRate"
                                        ),
                                        Map.of("fieldName", "amountCurrency", "type", "INTEGER"),
                                        Map.of("fieldName", "orderDate", "type", "STRING")
                                )
                        ))),
                new LowCodePackageDependencyManifest(List.of(LowCodePackageDependency.exchangeRateType("SPOT"))),
                null
        );
        LowCodeModuleHealthService service = new LowCodeModuleHealthService(
                List.of(new LowCodeMoneyHealthChecker()));

        LowCodeConfigHealthReport report = service.check(LowCodeModuleHealthContext.ofPackage(modulePackage));

        assertThat(report.status()).isEqualTo(LowCodeConfigHealthStatus.FAIL);
        assertThat(report.items()).extracting(LowCodeConfigHealthItem::code)
                .containsExactlyInAnyOrder(
                        "MONEY_CURRENCY_COMPANION_NOT_TEXT",
                        "MONEY_BASE_CURRENCY_INVALID",
                        "MONEY_BASE_AMOUNT_CONFLICT",
                        "MONEY_RATE_TYPE_INVALID",
                        "MONEY_RATE_DATE_FIELD_NOT_DATE",
                        "MONEY_EXCHANGE_RATE_FIELD_MISSING"
                );
    }

    @Test
    void moneyCheckerShouldRejectConfigurationBaseAmountIdPointingToOwner() {
        LowCodeModulePackage modulePackage = new LowCodeModulePackage(
                "m10.v1",
                LowCodePackageMode.MODULE_FULL,
                "crm",
                "crm.contract",
                List.of(LowCodeConfigBundle.included(LowCodePackageBundleType.METADATA,
                        Map.of(
                                "module", "crm.contract",
                                "metadataFields", List.of(
                                        Map.of("id", "f_amount", "fieldName", "amount", "type", "DECIMAL"),
                                        Map.of("id", "f_currency", "fieldName", "amountCurrency", "type", "STRING")
                                ),
                                "moduleFields", List.of(
                                        Map.of(
                                                "metadataFieldId", "f_amount",
                                                "moneyCurrencyMode", "SELECTABLE",
                                                "moneyCurrencyFieldId", "f_currency",
                                                "moneyBaseAmountFieldId", "f_amount",
                                                "moneyRateTypeCode", "SPOT"
                                        )
                                )
                        ))),
                new LowCodePackageDependencyManifest(List.of(LowCodePackageDependency.exchangeRateType("SPOT"))),
                null
        );
        LowCodeModuleHealthService service = new LowCodeModuleHealthService(
                List.of(new LowCodeMoneyHealthChecker()));

        LowCodeConfigHealthReport report = service.check(LowCodeModuleHealthContext.ofPackage(modulePackage));

        assertThat(report.status()).isEqualTo(LowCodeConfigHealthStatus.FAIL);
        assertThat(report.items()).extracting(LowCodeConfigHealthItem::code, LowCodeConfigHealthItem::targetId)
                .containsExactly(tuple("MONEY_BASE_AMOUNT_CONFLICT", "amount"));
    }

    @Test
    void measureUnitCheckerShouldSupportRuntimeNestedMeasureUnitContract() {
        LowCodeModulePackage modulePackage = new LowCodeModulePackage(
                "m10.v1",
                LowCodePackageMode.MODULE_FULL,
                "crm",
                "crm.contract",
                List.of(LowCodeConfigBundle.included(LowCodePackageBundleType.METADATA,
                        Map.of(
                                "module", "crm.contract",
                                "fields", List.of(
                                        Map.of(
                                                "fieldName", "quantity",
                                                "measureUnit", Map.of(
                                                        "categoryAlias", "quantity",
                                                        "mode", "SELECTABLE",
                                                        "baseUnitCode", "bottle",
                                                        "baseValueFieldName", "quantityBase",
                                                        "unitFieldName", "quantityUnit"
                                                )
                                        ),
                                        Map.of("fieldName", "quantityUnit"),
                                        Map.of("fieldName", "quantityBase")
                                )
                        ))),
                new LowCodePackageDependencyManifest(List.of(LowCodePackageDependency.measureUnit("crm", "quantity"))),
                null
        );
        LowCodeModuleHealthService service = new LowCodeModuleHealthService(
                List.of(new LowCodeMeasureUnitHealthChecker()));

        LowCodeConfigHealthReport report = service.check(LowCodeModuleHealthContext.ofPackage(modulePackage));

        assertThat(report.status()).isEqualTo(LowCodeConfigHealthStatus.PASS);
    }

    @Test
    void measureUnitCheckerShouldResolveConfigurationFieldIds() {
        LowCodeModulePackage modulePackage = new LowCodeModulePackage(
                "m10.v1",
                LowCodePackageMode.MODULE_FULL,
                "crm",
                "crm.contract",
                List.of(LowCodeConfigBundle.included(LowCodePackageBundleType.METADATA,
                        Map.of(
                                "module", "crm.contract",
                                "metadataFields", List.of(
                                        Map.of("id", "f_qty", "fieldName", "quantity"),
                                        Map.of("id", "f_unit", "fieldName", "quantityUnit"),
                                        Map.of("id", "f_base", "fieldName", "quantityBase"),
                                        Map.of("id", "f_sku", "fieldName", "skuId")
                                ),
                                "moduleFields", List.of(
                                        Map.of(
                                                "metadataFieldId", "f_qty",
                                                "fieldName", "quantity",
                                                "unitCategoryAlias", "quantity",
                                                "unitMode", "SELECTABLE",
                                                "baseUnitCode", "bottle",
                                                "baseValueFieldId", "f_base",
                                                "unitFieldId", "f_unit",
                                                "conversionScopeFieldId", "f_sku"
                                        )
                                )
                        ))),
                new LowCodePackageDependencyManifest(List.of(LowCodePackageDependency.measureUnit("crm", "quantity"))),
                null
        );
        LowCodeModuleHealthService service = new LowCodeModuleHealthService(
                List.of(new LowCodeMeasureUnitHealthChecker()));

        LowCodeConfigHealthReport report = service.check(LowCodeModuleHealthContext.ofPackage(modulePackage));

        assertThat(report.status()).isEqualTo(LowCodeConfigHealthStatus.PASS);
    }

    @Test
    void measureUnitCheckerShouldRejectConfigurationBaseValueIdPointingToOwner() {
        LowCodeModulePackage modulePackage = new LowCodeModulePackage(
                "m10.v1",
                LowCodePackageMode.MODULE_FULL,
                "crm",
                "crm.contract",
                List.of(LowCodeConfigBundle.included(LowCodePackageBundleType.METADATA,
                        Map.of(
                                "module", "crm.contract",
                                "metadataFields", List.of(
                                        Map.of("id", "f_qty", "fieldName", "quantity"),
                                        Map.of("id", "f_unit", "fieldName", "quantityUnit")
                                ),
                                "moduleFields", List.of(
                                        Map.of(
                                                "metadataFieldId", "f_qty",
                                                "unitCategoryAlias", "quantity",
                                                "unitMode", "SELECTABLE",
                                                "baseUnitCode", "bottle",
                                                "baseValueFieldId", "f_qty",
                                                "unitFieldId", "f_unit"
                                        )
                                )
                        ))),
                new LowCodePackageDependencyManifest(List.of(LowCodePackageDependency.measureUnit("crm", "quantity"))),
                null
        );
        LowCodeModuleHealthService service = new LowCodeModuleHealthService(
                List.of(new LowCodeMeasureUnitHealthChecker()));

        LowCodeConfigHealthReport report = service.check(LowCodeModuleHealthContext.ofPackage(modulePackage));

        assertThat(report.status()).isEqualTo(LowCodeConfigHealthStatus.FAIL);
        assertThat(report.items()).extracting(LowCodeConfigHealthItem::code, LowCodeConfigHealthItem::targetId)
                .containsExactly(tuple("MEASURE_UNIT_BASE_VALUE_CONFLICT", "quantity"));
    }

    @Test
    void measureUnitCheckerShouldRequireBaseUnitCategoryDependencyWhenDifferent() {
        LowCodeModulePackage modulePackage = new LowCodeModulePackage(
                "m10.v1",
                LowCodePackageMode.MODULE_FULL,
                "crm",
                "crm.contract",
                List.of(LowCodeConfigBundle.included(LowCodePackageBundleType.METADATA,
                        Map.of(
                                "module", "crm.contract",
                                "fields", List.of(
                                        Map.of(
                                                "fieldName", "quantity",
                                                "unitCategoryAlias", "package",
                                                "baseUnitCategoryAlias", "quantity",
                                                "unitMode", "FIXED",
                                                "fixedUnitCode", "box",
                                                "baseUnitCode", "bottle",
                                                "baseValueFieldName", "quantityBase"
                                        ),
                                        Map.of("fieldName", "quantityBase")
                                )
                        ))),
                new LowCodePackageDependencyManifest(List.of(LowCodePackageDependency.measureUnit("crm", "package"))),
                null
        );
        LowCodeModuleHealthService service = new LowCodeModuleHealthService(
                List.of(new LowCodeMeasureUnitHealthChecker()));

        LowCodeConfigHealthReport report = service.check(LowCodeModuleHealthContext.ofPackage(modulePackage));

        assertThat(report.status()).isEqualTo(LowCodeConfigHealthStatus.WARN);
        assertThat(report.items()).extracting(LowCodeConfigHealthItem::code, LowCodeConfigHealthItem::targetId)
                .containsExactly(tuple("MEASURE_UNIT_DEPENDENCY_MISSING", "quantity"));
    }

    @Test
    void measureUnitCheckerShouldReportBrokenMetadataFieldContract() {
        LowCodeModulePackage modulePackage = new LowCodeModulePackage(
                "m10.v1",
                LowCodePackageMode.MODULE_FULL,
                "crm",
                "crm.contract",
                List.of(LowCodeConfigBundle.included(LowCodePackageBundleType.METADATA,
                        Map.of(
                                "module", "crm.contract",
                                "fields", List.of(
                                        Map.of(
                                                "fieldName", "quantity",
                                                "unitCategoryAlias", "quantity",
                                                "unitMode", "SELECTABLE",
                                                "baseUnitCode", "bottle",
                                                "baseValueFieldName", "quantity",
                                                "unitFieldName", "quantityUnit",
                                                "conversionScopeFieldName", "skuId"
                                        ),
                                        Map.of("fieldName", "quantityUnit")
                                )
                        ))),
                new LowCodePackageDependencyManifest(List.of(LowCodePackageDependency.measureUnit("crm", "quantity"))),
                null
        );
        LowCodeModuleHealthService service = new LowCodeModuleHealthService(
                List.of(new LowCodeMeasureUnitHealthChecker()));

        LowCodeConfigHealthReport report = service.check(LowCodeModuleHealthContext.ofPackage(modulePackage));

        assertThat(report.status()).isEqualTo(LowCodeConfigHealthStatus.FAIL);
        assertThat(report.items()).extracting(LowCodeConfigHealthItem::code)
                .containsExactlyInAnyOrder("MEASURE_UNIT_BASE_VALUE_CONFLICT", "MEASURE_UNIT_SCOPE_FIELD_MISSING");
    }

    @Test
    void measureUnitCheckerShouldRejectIncompatibleFieldTypes() {
        LowCodeModulePackage modulePackage = new LowCodeModulePackage(
                "m10.v1",
                LowCodePackageMode.MODULE_FULL,
                "crm",
                "crm.contract",
                List.of(LowCodeConfigBundle.included(LowCodePackageBundleType.METADATA,
                        Map.of(
                                "module", "crm.contract",
                                "fields", List.of(
                                        Map.of(
                                                "fieldName", "quantity",
                                                "fieldType", "STRING",
                                                "unitCategoryAlias", "quantity",
                                                "unitMode", "SELECTABLE",
                                                "baseUnitCode", "bottle",
                                                "baseValueFieldName", "quantityBase",
                                                "unitFieldName", "quantityUnit"
                                        ),
                                        Map.of("fieldName", "quantityUnit", "fieldType", "DECIMAL"),
                                        Map.of("fieldName", "quantityBase", "fieldType", "STRING")
                                )
                        ))),
                new LowCodePackageDependencyManifest(List.of(LowCodePackageDependency.measureUnit("crm", "quantity"))),
                null
        );
        LowCodeModuleHealthService service = new LowCodeModuleHealthService(
                List.of(new LowCodeMeasureUnitHealthChecker()));

        LowCodeConfigHealthReport report = service.check(LowCodeModuleHealthContext.ofPackage(modulePackage));

        assertThat(report.status()).isEqualTo(LowCodeConfigHealthStatus.FAIL);
        assertThat(report.items()).extracting(LowCodeConfigHealthItem::code)
                .containsExactlyInAnyOrder(
                        "MEASURE_UNIT_OWNER_NOT_NUMERIC",
                        "MEASURE_UNIT_COMPANION_NOT_TEXT",
                        "MEASURE_UNIT_BASE_VALUE_NOT_NUMERIC"
                );
    }

    @Test
    void dependencyCheckerShouldResolveMeasureUnitCategoryDependency() {
        LowCodeModulePackage modulePackage = measureUnitPackage(
                List.of(LowCodePackageDependency.measureUnit("crm", "quantity")));
        LowCodeModuleHealthService service = new LowCodeModuleHealthService(
                List.of(new LowCodeModuleDependencyHealthChecker(List.of(new LowCodeConfigTestFixtures.RecordingDependencyResolver(
                        Set.of(LowCodePackageDependencyType.MEASURE_UNIT), Set.of()
                )))));

        LowCodeConfigHealthReport report = service.check(LowCodeModuleHealthContext.ofPackage(modulePackage));

        assertThat(report.status()).isEqualTo(LowCodeConfigHealthStatus.FAIL);
        assertThat(report.items().getFirst().code()).isEqualTo("REQUIRED_DEPENDENCY_MISSING");
        assertThat(report.items().getFirst().targetId()).isEqualTo("crm:quantity");
    }

    @Test
    void shouldWireHealthServiceWithPackageCheckerInSpringContext() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.register(LowCodeModuleHealthService.class,
                    LowCodeModulePackageHealthChecker.class,
                    LowCodeModuleBundleIdentityHealthChecker.class,
                    LowCodeModuleDependencyHealthChecker.class,
                    LowCodeMoneyHealthChecker.class,
                    LowCodeMeasureUnitHealthChecker.class);
            context.refresh();
            LowCodeModuleHealthService service = context.getBean(LowCodeModuleHealthService.class);

            LowCodeConfigHealthReport report = service.check(LowCodeModuleHealthContext.ofPackage(
                    fullPackageWithPageBundle("crm.contract")));

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

    private LowCodeModulePackage measureUnitPackage(List<LowCodePackageDependency> dependencies) {
        return new LowCodeModulePackage(
                "m10.v1",
                LowCodePackageMode.MODULE_FULL,
                "crm",
                "crm.contract",
                List.of(LowCodeConfigBundle.included(LowCodePackageBundleType.METADATA,
                        Map.of(
                                "module", "crm.contract",
                                "fields", List.of(
                                        Map.of(
                                                "fieldName", "quantity",
                                                "unitCategoryAlias", "quantity",
                                                "unitMode", "SELECTABLE",
                                                "baseUnitCode", "bottle",
                                                "baseValueFieldName", "quantityBase",
                                                "unitFieldName", "quantityUnit",
                                                "conversionScopeFieldName", "skuId"
                                        ),
                                        Map.of("fieldName", "quantityUnit"),
                                        Map.of("fieldName", "quantityBase"),
                                        Map.of("fieldName", "skuId")
                                )
                        ))),
                new LowCodePackageDependencyManifest(dependencies),
                null
        );
    }

    private LowCodeModulePackage moneyPackage(List<LowCodePackageDependency> dependencies) {
        return new LowCodeModulePackage(
                "m10.v1",
                LowCodePackageMode.MODULE_FULL,
                "crm",
                "crm.contract",
                List.of(LowCodeConfigBundle.included(LowCodePackageBundleType.METADATA,
                        Map.of(
                                "module", "crm.contract",
                                "fields", List.of(
                                        Map.of(
                                                "fieldName", "amount",
                                                "type", "DECIMAL",
                                                "currencyMode", "SELECTABLE",
                                                "defaultCurrencyCode", "USD",
                                                "currencyFieldName", "amountCurrency",
                                                "baseAmountFieldName", "amountBase",
                                                "baseCurrencyCode", "CNY",
                                                "rateTypeCode", "SPOT",
                                                "rateDateFieldName", "orderDate",
                                                "exchangeRateFieldName", "amountExchangeRate"
                                        ),
                                        Map.of("fieldName", "amountCurrency", "type", "STRING"),
                                        Map.of("fieldName", "amountBase", "type", "DECIMAL"),
                                        Map.of("fieldName", "orderDate", "type", "DATE"),
                                        Map.of("fieldName", "amountExchangeRate", "type", "DECIMAL")
                                )
                        ))),
                new LowCodePackageDependencyManifest(dependencies),
                null
        );
    }

    private List<LowCodePackageDependency> moneyDependencies() {
        return List.of(
                LowCodePackageDependency.currency("USD"),
                LowCodePackageDependency.currency("CNY"),
                LowCodePackageDependency.exchangeRateType("SPOT")
        );
    }
}
