package net.ximatai.muyun.spring.boot.platform;

import net.ximatai.muyun.spring.platform.config.LowCodeConfigBundle;
import net.ximatai.muyun.spring.platform.config.LowCodeConfigHealthReport;
import net.ximatai.muyun.spring.platform.config.LowCodeConfigHealthStatus;
import net.ximatai.muyun.spring.platform.config.LowCodeModuleConfigArchiveFacade;
import net.ximatai.muyun.spring.platform.config.LowCodeModuleConfigArchiveResult;
import net.ximatai.muyun.spring.platform.config.LowCodeModuleConfigVersion;
import net.ximatai.muyun.spring.platform.config.LowCodeModuleHealthContext;
import net.ximatai.muyun.spring.platform.config.LowCodeModuleHealthService;
import net.ximatai.muyun.spring.platform.config.LowCodeModulePackage;
import net.ximatai.muyun.spring.platform.config.LowCodeModulePackageExchangeService;
import net.ximatai.muyun.spring.platform.config.LowCodeModulePackageImportDraft;
import net.ximatai.muyun.spring.platform.config.LowCodeModulePackageImportService;
import net.ximatai.muyun.spring.platform.config.LowCodeModuleTemplate;
import net.ximatai.muyun.spring.platform.config.LowCodeModuleTemplateInstantiationRequest;
import net.ximatai.muyun.spring.platform.config.LowCodeModuleTemplateService;
import net.ximatai.muyun.spring.platform.config.LowCodePackageBundleType;
import net.ximatai.muyun.spring.platform.config.LowCodePackageDryRunResult;
import net.ximatai.muyun.spring.platform.config.LowCodePackageDryRunStatus;
import net.ximatai.muyun.spring.platform.config.LowCodePackageMode;
import net.ximatai.muyun.spring.platform.config.LowCodePackageExchangeManifest;
import net.ximatai.muyun.spring.common.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LowCodeGovernanceWebControllerTest {
    @BeforeEach
    void setUp() {
        TenantContext.setTenantId("tenant-a");
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void shouldCheckPackageHealthThroughGovernanceEndpoint() {
        LowCodeModuleHealthService healthService = mock(LowCodeModuleHealthService.class);
        LowCodeConfigHealthReport report = LowCodeConfigHealthReport.of("crm.contract", List.of());
        when(healthService.check(any(LowCodeModuleHealthContext.class))).thenReturn(report);

        LowCodeConfigHealthReport response = controller(healthService).checkPackageHealth(modulePackage());

        assertThat(response.moduleAlias()).isEqualTo("crm.contract");
        assertThat(response.status()).isEqualTo(LowCodeConfigHealthStatus.PASS);

        ArgumentCaptor<LowCodeModuleHealthContext> captor =
                ArgumentCaptor.forClass(LowCodeModuleHealthContext.class);
        verify(healthService).check(captor.capture());
        assertThat(captor.getValue().moduleAlias()).isEqualTo("crm.contract");
    }

    @Test
    void shouldArchivePackageThroughArchiveFacade() {
        LowCodeModuleConfigArchiveFacade archiveFacade = mock(LowCodeModuleConfigArchiveFacade.class);
        LowCodeModuleConfigVersion version = version("version-1");
        when(archiveFacade.archive(any(LowCodeModulePackage.class), any(), any()))
                .thenReturn(new LowCodeModuleConfigArchiveResult(version,
                        LowCodeConfigHealthReport.of("crm.contract", List.of())));

        LowCodeModuleConfigArchiveResult response = controller(archiveFacade).archivePackage(
                new LowCodeGovernanceWebController.ArchivePackageRequest(modulePackage(), "u-1", "归档"));

        assertThat(response.version().getId()).isEqualTo("version-1");
        assertThat(response.version().getModuleAlias()).isEqualTo("crm.contract");

        ArgumentCaptor<LowCodeModulePackage> packageCaptor = ArgumentCaptor.forClass(LowCodeModulePackage.class);
        verify(archiveFacade).archive(packageCaptor.capture(), org.mockito.ArgumentMatchers.eq("u-1"),
                org.mockito.ArgumentMatchers.eq("归档"));
        assertThat(packageCaptor.getValue().moduleAlias()).isEqualTo("crm.contract");
    }

    @Test
    void shouldSwitchCurrentPackageVersionThroughArchiveFacade() {
        LowCodeModuleConfigArchiveFacade archiveFacade = mock(LowCodeModuleConfigArchiveFacade.class);
        when(archiveFacade.switchCurrentVersion("crm.contract", "version-1")).thenReturn(version("version-1"));

        LowCodeModuleConfigVersion response =
                controller(archiveFacade).switchCurrentPackageVersion("crm.contract", "version-1");

        assertThat(response.getId()).isEqualTo("version-1");
        assertThat(response.getCurrentVersion()).isTrue();

        verify(archiveFacade).switchCurrentVersion("crm.contract", "version-1");
    }

    @Test
    void shouldExportCurrentAndVersionPackageThroughExchangeService() {
        LowCodeModulePackageExchangeService exchangeService = mock(LowCodeModulePackageExchangeService.class);
        LowCodeModulePackage modulePackage = modulePackage();
        when(exchangeService.exportCurrentPackage("crm.contract")).thenReturn(packageJson());
        when(exchangeService.exportVersionPackage("version-1")).thenReturn(packageJson());
        when(exchangeService.parsePackage(packageJson())).thenReturn(modulePackage);

        LowCodeGovernanceWebController controller = controller(exchangeService);

        assertThat(controller.exportCurrentPackage("crm.contract").moduleAlias()).isEqualTo("crm.contract");
        assertThat(controller.exportVersionPackage("version-1").moduleAlias()).isEqualTo("crm.contract");

        verify(exchangeService).exportCurrentPackage("crm.contract");
        verify(exchangeService).exportVersionPackage("version-1");
    }

    @Test
    void shouldPrepareAndArchiveImportDraftThroughImportService() {
        LowCodeModulePackageExchangeService exchangeService = mock(LowCodeModulePackageExchangeService.class);
        LowCodeModulePackageImportService importService = mock(LowCodeModulePackageImportService.class);
        LowCodeModulePackage modulePackage = modulePackage();
        LowCodePackageDryRunResult dryRun = new LowCodePackageDryRunResult(
                modulePackage, null, LowCodeConfigHealthReport.of("crm.contract", List.of()), List.of());
        LowCodeModulePackageImportDraft draft = new LowCodeModulePackageImportDraft(
                "draft-1", modulePackage, dryRun, null, Instant.EPOCH);
        when(exchangeService.dryRunImport(any(LowCodeModulePackage.class))).thenReturn(dryRun);
        when(importService.prepareDraft(any(LowCodeModulePackage.class))).thenReturn(draft);
        when(importService.archiveDraft(any(LowCodeModulePackageImportDraft.class), any(), any()))
                .thenReturn(new LowCodeModuleConfigArchiveResult(version("version-1"), dryRun.healthReport()));

        LowCodeGovernanceWebController controller = controller(exchangeService, importService);

        assertThat(controller.dryRunImportPackage(modulePackage).status()).isEqualTo(LowCodePackageDryRunStatus.READY);
        assertThat(controller.prepareImportDraft(modulePackage).draftId()).isEqualTo("draft-1");
        LowCodeModuleConfigArchiveResult response = controller.archiveImportDraft(
                new LowCodeGovernanceWebController.ArchiveImportDraftRequest(draft, "u-1", "导入"));
        assertThat(response.version().getId()).isEqualTo("version-1");

        verify(exchangeService).dryRunImport(any(LowCodeModulePackage.class));
        verify(importService).prepareDraft(any(LowCodeModulePackage.class));
        verify(importService).archiveDraft(any(LowCodeModulePackageImportDraft.class),
                org.mockito.ArgumentMatchers.eq("u-1"), org.mockito.ArgumentMatchers.eq("导入"));
    }

    @Test
    void shouldCreateAndInstantiateTemplateThroughGovernanceEndpoint() {
        LowCodeModuleTemplateService templateService = mock(LowCodeModuleTemplateService.class);
        LowCodeModuleTemplate template = template();
        LowCodeModulePackage instantiated = new LowCodeModulePackage(
                "1.0",
                LowCodePackageMode.MODULE_FULL,
                "sales",
                "sales.contract",
                List.of(LowCodeConfigBundle.included(LowCodePackageBundleType.METADATA,
                        Map.of("moduleAlias", "sales.contract"))),
                null,
                LowCodePackageExchangeManifest.draft("1.0"));
        when(templateService.createTemplateFromVersion("contract_template", "Contract Template", "version-1"))
                .thenReturn(template);
        when(templateService.instantiate(any(LowCodeModuleTemplate.class),
                any(LowCodeModuleTemplateInstantiationRequest.class))).thenReturn(instantiated);

        LowCodeGovernanceWebController controller = controller(templateService);

        LowCodeModuleTemplate response = controller.createTemplateFromVersion(
                new LowCodeGovernanceWebController.CreateTemplateFromVersionRequest(
                        "contract_template", "Contract Template", "version-1"));
        assertThat(response.templateAlias()).isEqualTo("contract_template");
        assertThat(response.basePackage().mode()).isEqualTo(LowCodePackageMode.TEMPLATE);

        LowCodeModulePackage instantiateResponse = controller.instantiateTemplate(
                new LowCodeGovernanceWebController.InstantiateTemplateRequest(template,
                        new LowCodeModuleTemplateInstantiationRequest(
                                "sales", "sales.contract", "Sales Contract",
                                Map.of("tableName", "sales_contract"))));
        assertThat(instantiateResponse.mode()).isEqualTo(LowCodePackageMode.MODULE_FULL);
        assertThat(instantiateResponse.moduleAlias()).isEqualTo("sales.contract");

        verify(templateService).createTemplateFromVersion("contract_template", "Contract Template", "version-1");
        ArgumentCaptor<LowCodeModuleTemplate> templateCaptor =
                ArgumentCaptor.forClass(LowCodeModuleTemplate.class);
        ArgumentCaptor<LowCodeModuleTemplateInstantiationRequest> requestCaptor =
                ArgumentCaptor.forClass(LowCodeModuleTemplateInstantiationRequest.class);
        verify(templateService).instantiate(templateCaptor.capture(), requestCaptor.capture());
        assertThat(templateCaptor.getValue().templateAlias()).isEqualTo("contract_template");
        assertThat(templateCaptor.getValue().basePackage().mode()).isEqualTo(LowCodePackageMode.TEMPLATE);
        assertThat(templateCaptor.getValue().basePackage().moduleAlias()).isEqualTo("crm.contract");
        assertThat(requestCaptor.getValue().moduleAlias()).isEqualTo("sales.contract");
        assertThat(requestCaptor.getValue().parameters()).containsEntry("tableName", "sales_contract");
    }

    private LowCodeGovernanceWebController controller(LowCodeModuleHealthService healthService) {
        return new LowCodeGovernanceWebController(
                mock(LowCodeModuleConfigArchiveFacade.class),
                healthService,
                mock(LowCodeModulePackageExchangeService.class),
                mock(LowCodeModulePackageImportService.class),
                mock(LowCodeModuleTemplateService.class));
    }

    private LowCodeGovernanceWebController controller(LowCodeModuleConfigArchiveFacade archiveFacade) {
        return new LowCodeGovernanceWebController(
                archiveFacade,
                mock(LowCodeModuleHealthService.class),
                mock(LowCodeModulePackageExchangeService.class),
                mock(LowCodeModulePackageImportService.class),
                mock(LowCodeModuleTemplateService.class));
    }

    private LowCodeGovernanceWebController controller(LowCodeModulePackageExchangeService exchangeService) {
        return controller(exchangeService, mock(LowCodeModulePackageImportService.class));
    }

    private LowCodeGovernanceWebController controller(LowCodeModulePackageExchangeService exchangeService,
                                                     LowCodeModulePackageImportService importService) {
        return new LowCodeGovernanceWebController(
                mock(LowCodeModuleConfigArchiveFacade.class),
                mock(LowCodeModuleHealthService.class),
                exchangeService,
                importService,
                mock(LowCodeModuleTemplateService.class));
    }

    private LowCodeGovernanceWebController controller(LowCodeModuleTemplateService templateService) {
        return new LowCodeGovernanceWebController(
                mock(LowCodeModuleConfigArchiveFacade.class),
                mock(LowCodeModuleHealthService.class),
                mock(LowCodeModulePackageExchangeService.class),
                mock(LowCodeModulePackageImportService.class),
                templateService);
    }

    private LowCodeModulePackage modulePackage() {
        return new LowCodeModulePackage(
                "1.0",
                LowCodePackageMode.MODULE_FULL,
                "crm",
                "crm.contract",
                List.of(LowCodeConfigBundle.included(LowCodePackageBundleType.METADATA,
                        Map.of("moduleAlias", "crm.contract"))),
                null,
                LowCodePackageExchangeManifest.draft("1.0"));
    }

    private LowCodeModuleTemplate template() {
        return new LowCodeModuleTemplate("contract_template", "Contract Template", templatePackage());
    }

    private LowCodeModulePackage templatePackage() {
        return new LowCodeModulePackage(
                "1.0",
                LowCodePackageMode.TEMPLATE,
                "crm",
                "crm.contract",
                List.of(LowCodeConfigBundle.included(LowCodePackageBundleType.METADATA,
                        Map.of("moduleAlias", "crm.contract"))),
                null,
                LowCodePackageExchangeManifest.draft("1.0"));
    }

    private LowCodeModuleConfigVersion version(String id) {
        LowCodeModuleConfigVersion version = new LowCodeModuleConfigVersion();
        version.setId(id);
        version.setModuleAlias("crm.contract");
        version.setVersionNo(1);
        version.setCurrentVersion(Boolean.TRUE);
        version.setPackageSnapshotText(packageJson());
        version.setPackageHash("hash");
        return version;
    }

    private String packageJson() {
        return """
                {
                  "protocolVersion": "1.0",
                  "mode": "MODULE_FULL",
                  "applicationAlias": "crm",
                  "moduleAlias": "crm.contract",
                  "bundles": [
                    {
                      "type": "METADATA",
                      "included": true,
                      "content": {"moduleAlias": "crm.contract"}
                    }
                  ]
                }
                """;
    }

    private String templatePackageJson() {
        return """
                {
                  "protocolVersion": "1.0",
                  "mode": "TEMPLATE",
                  "applicationAlias": "crm",
                  "moduleAlias": "crm.contract",
                  "bundles": [
                    {
                      "type": "METADATA",
                      "included": true,
                      "content": {"moduleAlias": "crm.contract"}
                    }
                  ]
                }
                """;
    }
}
