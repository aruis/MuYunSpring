package net.ximatai.muyun.spring.platform.config;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class LowCodeConfigTestFixtures {
    static final String PROTOCOL_VERSION = "m10.v1";
    static final String CRM_CONTRACT = "crm.contract";
    static final String SALES_CONTRACT = "sales.contract";

    private LowCodeConfigTestFixtures() {
    }

    static LowCodeModulePackage fullPackage(String moduleAlias) {
        return fullPackage(moduleAlias, List.of());
    }

    static LowCodeModulePackage fullPackage(String moduleAlias, List<LowCodePackageDependency> dependencies) {
        String applicationAlias = applicationAlias(moduleAlias);
        return new LowCodeModulePackage(
                PROTOCOL_VERSION,
                LowCodePackageMode.MODULE_FULL,
                applicationAlias,
                moduleAlias,
                List.of(LowCodeConfigBundle.included(LowCodePackageBundleType.METADATA,
                        Map.of("module", moduleAlias, "title", "Contract"))),
                new LowCodePackageDependencyManifest(dependencies),
                null
        );
    }

    static LowCodeModulePackage fullPackageWithPageBundle(String moduleAlias) {
        String applicationAlias = applicationAlias(moduleAlias);
        return new LowCodeModulePackage(
                PROTOCOL_VERSION,
                LowCodePackageMode.MODULE_FULL,
                applicationAlias,
                moduleAlias,
                List.of(
                        LowCodeConfigBundle.included(LowCodePackageBundleType.METADATA,
                                Map.of("module", moduleAlias, "title", "Contract")),
                        LowCodeConfigBundle.included(LowCodePackageBundleType.PAGE,
                                Map.of("moduleAlias", moduleAlias, "uiConfigs", List.of("list")))
                ),
                null,
                null
        );
    }

    static LowCodeModulePackage pageOnlyPackage(String moduleAlias) {
        String applicationAlias = applicationAlias(moduleAlias);
        return new LowCodeModulePackage(
                PROTOCOL_VERSION,
                LowCodePackageMode.PAGE_ONLY,
                applicationAlias,
                moduleAlias,
                List.of(LowCodeConfigBundle.included(LowCodePackageBundleType.PAGE,
                        Map.of("moduleAlias", moduleAlias, "uiConfigs", List.of("list")))),
                null,
                null
        );
    }

    static LowCodeModulePackage templatePackage(String moduleAlias) {
        String applicationAlias = applicationAlias(moduleAlias);
        return new LowCodeModulePackage(
                PROTOCOL_VERSION,
                LowCodePackageMode.TEMPLATE,
                applicationAlias,
                moduleAlias,
                List.of(LowCodeConfigBundle.included(LowCodePackageBundleType.METADATA,
                        Map.of("module", moduleAlias, "title", "Contract"))),
                null,
                null
        );
    }

    static LowCodeModulePackage salesContractPackage(String title, String tableName, String defaultStatus) {
        return new LowCodeModulePackage(
                PROTOCOL_VERSION,
                LowCodePackageMode.MODULE_FULL,
                "sales",
                SALES_CONTRACT,
                List.of(
                        LowCodeConfigBundle.included(LowCodePackageBundleType.METADATA, Map.of(
                                "module", SALES_CONTRACT,
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
                                "menu", SALES_CONTRACT,
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

    static RecordingDependencyResolver completeDependencyResolver() {
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

    static Set<String> dependencyKeys() {
        return Set.of(
                "MODULE:sales.customer",
                "DICTIONARY:sales:contract_status",
                "ACTION:finance.invoice:createFromContract",
                "WORKFLOW:contract_approval",
                "FILE_SERVICE:record_attachment",
                "EXTERNAL:erp_credit_check"
        );
    }

    static Set<String> dependencyKeysWithout(String missingKey) {
        Set<String> keys = new LinkedHashSet<>(dependencyKeys());
        keys.remove(missingKey);
        return keys;
    }

    static String dependencyKey(LowCodePackageDependency dependency) {
        return switch (dependency.type()) {
            case MODULE -> "MODULE:" + dependency.moduleAlias();
            case DICTIONARY -> "DICTIONARY:" + dependency.applicationAlias() + ":" + dependency.alias();
            case ACTION -> "ACTION:" + dependency.moduleAlias() + ":" + dependency.alias();
            case WORKFLOW -> "WORKFLOW:" + dependency.alias();
            case FILE_SERVICE -> "FILE_SERVICE:" + dependency.alias();
            case EXTERNAL -> "EXTERNAL:" + dependency.alias();
        };
    }

    private static String applicationAlias(String moduleAlias) {
        return moduleAlias.substring(0, moduleAlias.indexOf('.'));
    }

    static final class RecordingDependencyResolver implements LowCodePackageDependencyResolver {
        private final Set<LowCodePackageDependencyType> supportedTypes;
        private final Set<String> existingKeys;
        private final List<String> resolvedKeys = new ArrayList<>();

        RecordingDependencyResolver(Set<LowCodePackageDependencyType> supportedTypes,
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

        List<String> resolvedKeys() {
            return List.copyOf(resolvedKeys);
        }
    }
}
