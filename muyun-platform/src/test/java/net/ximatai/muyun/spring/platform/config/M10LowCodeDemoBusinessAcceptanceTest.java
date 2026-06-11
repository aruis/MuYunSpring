package net.ximatai.muyun.spring.platform.config;

import net.ximatai.muyun.spring.platform.support.TestMemoryDao;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class M10LowCodeDemoBusinessAcceptanceTest {
    private final LowCodeModuleConfigVersionService sourceVersionService =
            new LowCodeModuleConfigVersionService(new TestMemoryDao<>());
    private final LowCodeModuleHealthService healthService =
            new LowCodeModuleHealthService(List.of(new LowCodeModulePackageHealthChecker()));
    private final LowCodeModuleConfigPublishFacade publishFacade =
            new LowCodeModuleConfigPublishFacade(sourceVersionService, healthService);
    private final LowCodeModulePackageExchangeService sourceExchangeService =
            exchangeService(sourceVersionService);
    private final LowCodeModuleTemplateService templateService =
            new LowCodeModuleTemplateService(sourceExchangeService);

    @Test
    void shouldAcceptSalesContractDemoAcrossM10GovernanceLoop() {
        RecordingDependencyResolver dependencyResolver = completeDependencyResolver();
        LowCodeModulePackageExchangeService targetExchangeService =
                exchangeService(new LowCodeModuleConfigVersionService(new TestMemoryDao<>()), dependencyResolver);
        LowCodeModulePackage baseline = salesContractPackage("合同管理", "sales_contract", "draft");

        LowCodeModuleConfigPublishResult firstPublish = publishFacade.publish(baseline, "demo-admin", "demo baseline");

        assertThat(firstPublish.healthReport().status()).isEqualTo(LowCodeConfigHealthStatus.PASS);
        assertThat(firstPublish.version().getVersionNo()).isEqualTo(1);
        assertThat(firstPublish.version().getCurrentVersion()).isTrue();
        assertThat(firstPublish.version().getPackageHash()).hasSize(64);
        assertThat(firstPublish.version().getSummaryJson())
                .contains("\"mode\":\"MODULE_FULL\"")
                .contains("\"healthStatus\":\"PASS\"");

        String exported = sourceExchangeService.exportCurrentPackage("sales.contract");
        LowCodeModulePackage exportedPackage = sourceExchangeService.parsePackage(exported);
        assertThat(exportedPackage.applicationAlias()).isEqualTo("sales");
        assertThat(exportedPackage.moduleAlias()).isEqualTo("sales.contract");
        assertThat(exportedPackage.bundles()).extracting(LowCodeConfigBundle::type)
                .containsExactlyInAnyOrder(
                        LowCodePackageBundleType.METADATA,
                        LowCodePackageBundleType.PAGE,
                        LowCodePackageBundleType.INTERACTION,
                        LowCodePackageBundleType.ENTRY,
                        LowCodePackageBundleType.AUTOMATION
                );
        assertThat(exportedPackage.dependencyManifest().dependencies())
                .extracting(LowCodePackageDependency::type)
                .containsExactlyInAnyOrder(
                        LowCodePackageDependencyType.MODULE,
                        LowCodePackageDependencyType.DICTIONARY,
                        LowCodePackageDependencyType.ACTION,
                        LowCodePackageDependencyType.WORKFLOW,
                        LowCodePackageDependencyType.FILE_SERVICE,
                        LowCodePackageDependencyType.EXTERNAL
                );
        assertThat(exportedPackage.bundleMap().get(LowCodePackageBundleType.INTERACTION).content())
                .containsEntry("permissionActions", List.of("view", "create", "update", "submit", "generateInvoice"));

        assertThat(targetExchangeService.dryRunImport(exported).status()).isEqualTo(LowCodePackageDryRunStatus.READY);
        assertThat(dependencyResolver.resolvedKeys()).containsExactlyInAnyOrderElementsOf(dependencyKeys());

        LowCodeModuleConfigVersion secondVersion = publishFacade
                .publish(salesContractPackage("合同归档", "sales_contract_v2", "archived"), "demo-admin", "demo v2")
                .version();
        assertThat(secondVersion.getVersionNo()).isEqualTo(2);
        assertThat(sourceVersionService.currentVersion("sales.contract").getId()).isEqualTo(secondVersion.getId());
        assertThat(sourceExchangeService.exportCurrentPackage("sales.contract"))
                .contains("sales_contract_v2")
                .isNotEqualTo(exported);

        LowCodeModuleConfigVersion rolledBack = publishFacade.rollback("sales.contract", firstPublish.version().getId());
        assertThat(rolledBack.getId()).isEqualTo(firstPublish.version().getId());
        assertThat(sourceVersionService.currentVersion("sales.contract").getId()).isEqualTo(firstPublish.version().getId());
        assertThat(sourceExchangeService.exportCurrentPackage("sales.contract")).isEqualTo(exported);

        LowCodeModuleTemplate template = templateService.createTemplateFromVersion(
                "sales_contract_template", "合同模块样板", firstPublish.version().getId());
        LowCodeModulePackage renewalPackage = templateService.instantiate(template,
                new LowCodeModuleTemplateInstantiationRequest(
                        "sales",
                        "sales.contract.renewal",
                        "续签合同",
                        Map.of("tableName", "sales_contract_renewal")
                ));

        LowCodePackageDryRunResult renewalDryRun = targetExchangeService.dryRunImport(renewalPackage);
        assertThat(renewalDryRun.status()).isEqualTo(LowCodePackageDryRunStatus.READY);
        assertThat(renewalPackage.mode()).isEqualTo(LowCodePackageMode.MODULE_FULL);
        assertThat(renewalPackage.moduleAlias()).isEqualTo("sales.contract.renewal");
        assertThat(renewalPackage.bundleMap().get(LowCodePackageBundleType.METADATA).content())
                .containsEntry("module", "sales.contract.renewal")
                .containsEntry("title", "续签合同")
                .containsEntry("tableName", "sales_contract_renewal");
    }

    @Test
    void shouldBlockDemoMigrationWhenRequiredDependencyIsMissing() {
        LowCodeModulePackage contractPackage = salesContractPackage("合同管理", "sales_contract", "draft");
        RecordingDependencyResolver dependencyResolver = new RecordingDependencyResolver(
                Set.of(
                        LowCodePackageDependencyType.MODULE,
                        LowCodePackageDependencyType.DICTIONARY,
                        LowCodePackageDependencyType.ACTION,
                        LowCodePackageDependencyType.WORKFLOW,
                        LowCodePackageDependencyType.FILE_SERVICE,
                        LowCodePackageDependencyType.EXTERNAL
                ),
                dependencyKeysWithout("MODULE:sales.customer")
        );
        LowCodeModulePackageExchangeService strictExchangeService = new LowCodeModulePackageExchangeService(
                new LowCodeModuleConfigVersionService(new TestMemoryDao<>()),
                healthService,
                List.of(dependencyResolver)
        );

        LowCodePackageDryRunResult result = strictExchangeService.dryRunImport(contractPackage);

        assertThat(result.status()).isEqualTo(LowCodePackageDryRunStatus.BLOCKED);
        assertThat(result.conflicts()).extracting(LowCodePackageImportConflict::conflictType)
                .contains(LowCodePackageConflictType.REQUIRED_DEPENDENCY_MISSING);
    }

    private LowCodeModulePackage salesContractPackage(String title, String tableName, String defaultStatus) {
        return new LowCodeModulePackage(
                "m10.v1",
                LowCodePackageMode.MODULE_FULL,
                "sales",
                "sales.contract",
                List.of(
                        LowCodeConfigBundle.included(LowCodePackageBundleType.METADATA, Map.of(
                                "module", "sales.contract",
                                "mainEntity", "contract",
                                "title", title,
                                "tableName", tableName,
                                "children", List.of("contractLine"),
                                "references", List.of("sales.customer"),
                                "statusField", "status",
                                "defaultStatus", defaultStatus
                        )),
                        LowCodeConfigBundle.included(LowCodePackageBundleType.PAGE, Map.of(
                                "list", "contractList",
                                "form", "contractForm",
                                "detail", "contractDetail",
                                "queryFields", List.of("contractNo", "customerId", "status", "signedDate"),
                                "columns", List.of("contractNo", "customerName", "amount", "status")
                        )),
                        LowCodeConfigBundle.included(LowCodePackageBundleType.INTERACTION, Map.of(
                                "associationViews", List.of("invoiceList", "writeBackHistory"),
                                "localEditActions", List.of("editBaseInfo"),
                                "moduleTasks", List.of("completeLines", "generateInvoice"),
                                "dialogActions", List.of("submit", "generateInvoice"),
                                "permissionActions", List.of("view", "create", "update", "submit", "generateInvoice")
                        )),
                        LowCodeConfigBundle.included(LowCodePackageBundleType.ENTRY, Map.of(
                                "menu", "sales.contract",
                                "clientType", "WEB",
                                "pageMode", "LIST_DETAIL"
                        )),
                        LowCodeConfigBundle.included(LowCodePackageBundleType.AUTOMATION, Map.of(
                                "codeRule", "sales_contract_no",
                                "generationRules", List.of("contract_to_invoice"),
                                "writeBackRules", List.of("invoice_status_to_contract"),
                                "workflow", "contract_approval",
                                "importExport", "contract_excel"
                        ))
                ),
                new LowCodePackageDependencyManifest(List.of(
                        LowCodePackageDependency.module("sales.customer"),
                        LowCodePackageDependency.dictionary("sales", "contract_status"),
                        LowCodePackageDependency.action("finance.invoice", "createFromContract"),
                        new LowCodePackageDependency(LowCodePackageDependencyType.WORKFLOW,
                                null, null, "contract_approval", true),
                        new LowCodePackageDependency(LowCodePackageDependencyType.FILE_SERVICE,
                                null, null, "record_attachment", true),
                        new LowCodePackageDependency(LowCodePackageDependencyType.EXTERNAL,
                                null, null, "erp_credit_check", true)
                )),
                null
        );
    }

    private LowCodeModulePackageExchangeService exchangeService(LowCodeModuleConfigVersionService versionService) {
        return exchangeService(versionService, completeDependencyResolver());
    }

    private LowCodeModulePackageExchangeService exchangeService(LowCodeModuleConfigVersionService versionService,
                                                               RecordingDependencyResolver dependencyResolver) {
        return new LowCodeModulePackageExchangeService(
                versionService,
                healthService,
                List.of(dependencyResolver)
        );
    }

    private RecordingDependencyResolver completeDependencyResolver() {
        return new RecordingDependencyResolver(
                Set.of(
                        LowCodePackageDependencyType.MODULE,
                        LowCodePackageDependencyType.DICTIONARY,
                        LowCodePackageDependencyType.ACTION,
                        LowCodePackageDependencyType.WORKFLOW,
                        LowCodePackageDependencyType.FILE_SERVICE,
                        LowCodePackageDependencyType.EXTERNAL
                ),
                dependencyKeys()
        );
    }

    private Set<String> dependencyKeys() {
        return Set.of(
                "MODULE:sales.customer",
                "DICTIONARY:sales:contract_status",
                "ACTION:finance.invoice:createFromContract",
                "WORKFLOW:contract_approval",
                "FILE_SERVICE:record_attachment",
                "EXTERNAL:erp_credit_check"
        );
    }

    private Set<String> dependencyKeysWithout(String missingKey) {
        Set<String> keys = new LinkedHashSet<>(dependencyKeys());
        keys.remove(missingKey);
        return keys;
    }

    private static final class RecordingDependencyResolver implements LowCodePackageDependencyResolver {
        private final Set<LowCodePackageDependencyType> supportedTypes;
        private final Set<String> existingKeys;
        private final List<String> resolvedKeys = new ArrayList<>();

        private RecordingDependencyResolver(Set<LowCodePackageDependencyType> supportedTypes,
                                            Set<String> existingKeys) {
            this.supportedTypes = Set.copyOf(supportedTypes);
            this.existingKeys = Set.copyOf(existingKeys);
        }

        @Override
        public boolean supports(LowCodePackageDependencyType type) {
            return supportedTypes.contains(type);
        }

        @Override
        public boolean exists(LowCodePackageDependency dependency) {
            String key = dependencyKey(dependency);
            resolvedKeys.add(key);
            return existingKeys.contains(key);
        }

        private List<String> resolvedKeys() {
            return List.copyOf(resolvedKeys);
        }
    }

    private static String dependencyKey(LowCodePackageDependency dependency) {
        return switch (dependency.type()) {
            case MODULE -> "MODULE:" + dependency.moduleAlias();
            case DICTIONARY -> "DICTIONARY:" + dependency.applicationAlias() + ":" + dependency.alias();
            case ACTION -> "ACTION:" + dependency.moduleAlias() + ":" + dependency.alias();
            case WORKFLOW -> "WORKFLOW:" + dependency.alias();
            case FILE_SERVICE -> "FILE_SERVICE:" + dependency.alias();
            case EXTERNAL -> "EXTERNAL:" + dependency.alias();
        };
    }
}
