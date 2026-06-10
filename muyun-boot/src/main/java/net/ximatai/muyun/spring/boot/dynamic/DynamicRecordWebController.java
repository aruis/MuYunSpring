package net.ximatai.muyun.spring.boot.dynamic;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.database.core.orm.PageResult;
import net.ximatai.muyun.database.core.orm.Sort;
import net.ximatai.muyun.spring.ability.DataScopeAbility;
import net.ximatai.muyun.spring.ability.OptimisticLockException;
import net.ximatai.muyun.spring.boot.web.ActionWeb;
import net.ximatai.muyun.spring.boot.web.CrudWeb;
import net.ximatai.muyun.spring.boot.web.EnableWeb;
import net.ximatai.muyun.spring.boot.web.ReferenceWeb;
import net.ximatai.muyun.spring.boot.web.TreeSortWebRequest;
import net.ximatai.muyun.spring.boot.web.TreeWeb;
import net.ximatai.muyun.spring.boot.web.WebCountResponse;
import net.ximatai.muyun.spring.boot.web.WebOutputSupport;
import net.ximatai.muyun.spring.boot.web.WebPageResponse;
import net.ximatai.muyun.spring.boot.web.WebQueryCondition;
import net.ximatai.muyun.spring.boot.web.WebQueryRequest;
import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.common.platform.ActionEndpoint;
import net.ximatai.muyun.spring.common.platform.ActionExecutionContext;
import net.ximatai.muyun.spring.common.platform.ActionExecutionContextHolder;
import net.ximatai.muyun.spring.common.platform.ActionExecutionPolicy;
import net.ximatai.muyun.spring.common.platform.PlatformAction;
import net.ximatai.muyun.spring.common.tenant.ActiveTenantVerifier;
import net.ximatai.muyun.spring.common.tenant.TenantContext;
import net.ximatai.muyun.spring.common.web.PlatformWebPathRules;
import net.ximatai.muyun.spring.common.identity.CurrentUser;
import net.ximatai.muyun.spring.common.identity.CurrentUserContext;
import net.ximatai.muyun.spring.platform.attachment.RecordAttachment;
import net.ximatai.muyun.spring.platform.attachment.RecordAttachmentCommand;
import net.ximatai.muyun.spring.platform.attachment.RecordAttachmentService;
import net.ximatai.muyun.spring.platform.code.CodeBusinessPreviewItem;
import net.ximatai.muyun.spring.platform.code.CodeBusinessPreviewService;
import net.ximatai.muyun.spring.platform.duplicate.RecordDuplicateCheckResult;
import net.ximatai.muyun.spring.platform.duplicate.RecordDuplicateCheckService;
import net.ximatai.muyun.spring.platform.generation.RecordGenerationCommitResult;
import net.ximatai.muyun.spring.platform.generation.RecordGenerationDraft;
import net.ximatai.muyun.spring.platform.generation.RecordGenerationResult;
import net.ximatai.muyun.spring.platform.generation.ReferenceRecordGenerationFacade;
import net.ximatai.muyun.spring.platform.metadata.ModuleMetadataFieldService;
import net.ximatai.muyun.spring.platform.metadata.RelationRole;
import net.ximatai.muyun.spring.platform.metadata.ResolvedModuleMetadataField;
import net.ximatai.muyun.spring.dynamic.descriptor.DynamicModuleDescriptor;
import net.ximatai.muyun.spring.dynamic.descriptor.DynamicActionDescriptor;
import net.ximatai.muyun.spring.dynamic.descriptor.DynamicEntityDescriptor;
import net.ximatai.muyun.spring.dynamic.descriptor.DynamicRelationDescriptor;
import net.ximatai.muyun.spring.dynamic.metadata.EntityActionLevel;
import net.ximatai.muyun.spring.dynamic.metadata.FieldDefinition;
import net.ximatai.muyun.spring.common.platform.EntityCapability;
import net.ximatai.muyun.spring.common.security.FieldOutputContext;
import net.ximatai.muyun.spring.dynamic.metadata.ModuleDefinitionException;
import net.ximatai.muyun.spring.dynamic.openapi.DynamicOpenApiDocument;
import net.ximatai.muyun.spring.dynamic.openapi.DynamicOpenApiGenerator;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicActionExecutionException;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicActionExecutionRequest;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicActionAvailability;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicEntityOperations;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicQueryCondition;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicRecord;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicRecordService;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicReferenceMatchMode;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicReferenceResolveMode;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicReferenceResolveRequest;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicReferenceResolveResponse;
import net.ximatai.muyun.spring.platform.ui.PlatformPageConfigSnapshot;
import net.ximatai.muyun.spring.platform.ui.PlatformPageConfigSnapshotService;
import net.ximatai.muyun.spring.platform.ui.PlatformQueryItemService;
import net.ximatai.muyun.spring.platform.ui.PlatformQueryTemplate;
import net.ximatai.muyun.spring.platform.ui.PlatformRecordNavigationContext;
import net.ximatai.muyun.spring.platform.ui.PlatformRecordNavigationMove;
import net.ximatai.muyun.spring.platform.ui.PlatformRecordNavigationService;
import net.ximatai.muyun.spring.platform.ui.PlatformUiConfig;
import net.ximatai.muyun.spring.platform.ui.PlatformUiConfigField;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;

@RestController
@RequestMapping("/{moduleAlias:[a-z][a-z0-9_]*(?:\\.[a-z][a-z0-9_]*)+}")
public class DynamicRecordWebController implements
        CrudWeb<DynamicRecord, DynamicEntityOperations>,
        EnableWeb<DynamicRecord, DynamicEntityOperations>,
        TreeWeb<DynamicRecord, DynamicEntityOperations>,
        ActionWeb<DynamicEntityOperations,
                DynamicWebActionRequest,
                DynamicActionDescriptor,
                DynamicWebActionAvailabilityResponse,
                DynamicWebActionExecutionResponse>,
        ReferenceWeb<DynamicEntityOperations,
                DynamicWebReferenceRequest,
                DynamicReferenceResolveResponse> {
    private final DynamicRecordService recordService;
    private final ActiveTenantVerifier activeTenantVerifier;
    private final CodeBusinessPreviewService codeBusinessPreviewService;
    private final ReferenceRecordGenerationFacade referenceRecordGenerationFacade;
    private final PlatformPageConfigSnapshotService pageConfigSnapshotService;
    private final PlatformQueryItemService queryItemService;
    private final ModuleMetadataFieldService moduleMetadataFieldService;
    private final RecordAttachmentService recordAttachmentService;
    private final RecordDuplicateCheckService duplicateCheckService;
    private final PlatformRecordNavigationService navigationService;
    private final DynamicOpenApiGenerator openApiGenerator = new DynamicOpenApiGenerator();

    public DynamicRecordWebController(DynamicRecordService recordService,
                                      ActiveTenantVerifier activeTenantVerifier) {
        this(recordService, activeTenantVerifier, (CodeBusinessPreviewService) null,
                (ReferenceRecordGenerationFacade) null);
    }

    @Autowired
    public DynamicRecordWebController(DynamicRecordService recordService,
                                      ActiveTenantVerifier activeTenantVerifier,
                                      ObjectProvider<CodeBusinessPreviewService> codeBusinessPreviewServiceProvider,
                                      ObjectProvider<ReferenceRecordGenerationFacade> referenceGenerationFacadeProvider,
                                      ObjectProvider<PlatformPageConfigSnapshotService> pageConfigSnapshotServiceProvider,
                                      ObjectProvider<PlatformQueryItemService> queryItemServiceProvider,
                                      ObjectProvider<ModuleMetadataFieldService> moduleMetadataFieldServiceProvider,
                                      ObjectProvider<RecordAttachmentService> recordAttachmentServiceProvider,
                                      ObjectProvider<RecordDuplicateCheckService> duplicateCheckServiceProvider,
                                      ObjectProvider<PlatformRecordNavigationService> navigationServiceProvider) {
        this(recordService, activeTenantVerifier,
                codeBusinessPreviewServiceProvider == null ? null : codeBusinessPreviewServiceProvider.getIfAvailable(),
                referenceGenerationFacadeProvider == null ? null : referenceGenerationFacadeProvider.getIfAvailable(),
                pageConfigSnapshotServiceProvider == null ? null : pageConfigSnapshotServiceProvider.getIfAvailable(),
                queryItemServiceProvider == null ? null : queryItemServiceProvider.getIfAvailable(),
                moduleMetadataFieldServiceProvider == null ? null : moduleMetadataFieldServiceProvider.getIfAvailable(),
                recordAttachmentServiceProvider == null ? null : recordAttachmentServiceProvider.getIfAvailable(),
                duplicateCheckServiceProvider == null ? null : duplicateCheckServiceProvider.getIfAvailable(),
                navigationServiceProvider == null ? null : navigationServiceProvider.getIfAvailable());
    }

    public DynamicRecordWebController(DynamicRecordService recordService,
                                      ActiveTenantVerifier activeTenantVerifier,
                                      CodeBusinessPreviewService codeBusinessPreviewService) {
        this(recordService, activeTenantVerifier, codeBusinessPreviewService, null);
    }

    public DynamicRecordWebController(DynamicRecordService recordService,
                                      ActiveTenantVerifier activeTenantVerifier,
                                      CodeBusinessPreviewService codeBusinessPreviewService,
                                      ReferenceRecordGenerationFacade referenceRecordGenerationFacade) {
        this(recordService, activeTenantVerifier, codeBusinessPreviewService, referenceRecordGenerationFacade,
                null, null, null, null, null, null);
    }

    public DynamicRecordWebController(DynamicRecordService recordService,
                                      ActiveTenantVerifier activeTenantVerifier,
                                      CodeBusinessPreviewService codeBusinessPreviewService,
                                      ReferenceRecordGenerationFacade referenceRecordGenerationFacade,
                                      PlatformPageConfigSnapshotService pageConfigSnapshotService,
                                      PlatformQueryItemService queryItemService,
                                      ModuleMetadataFieldService moduleMetadataFieldService) {
        this(recordService, activeTenantVerifier, codeBusinessPreviewService, referenceRecordGenerationFacade,
                pageConfigSnapshotService, queryItemService, moduleMetadataFieldService, null, null, null);
    }

    public DynamicRecordWebController(DynamicRecordService recordService,
                                      ActiveTenantVerifier activeTenantVerifier,
                                      CodeBusinessPreviewService codeBusinessPreviewService,
                                      ReferenceRecordGenerationFacade referenceRecordGenerationFacade,
                                      PlatformPageConfigSnapshotService pageConfigSnapshotService,
                                      PlatformQueryItemService queryItemService,
                                      ModuleMetadataFieldService moduleMetadataFieldService,
                                      RecordAttachmentService recordAttachmentService) {
        this(recordService, activeTenantVerifier, codeBusinessPreviewService, referenceRecordGenerationFacade,
                pageConfigSnapshotService, queryItemService, moduleMetadataFieldService, recordAttachmentService, null, null);
    }

    public DynamicRecordWebController(DynamicRecordService recordService,
                                      ActiveTenantVerifier activeTenantVerifier,
                                      CodeBusinessPreviewService codeBusinessPreviewService,
                                      ReferenceRecordGenerationFacade referenceRecordGenerationFacade,
                                      PlatformPageConfigSnapshotService pageConfigSnapshotService,
                                      PlatformQueryItemService queryItemService,
                                      ModuleMetadataFieldService moduleMetadataFieldService,
                                      RecordAttachmentService recordAttachmentService,
                                      RecordDuplicateCheckService duplicateCheckService) {
        this(recordService, activeTenantVerifier, codeBusinessPreviewService, referenceRecordGenerationFacade,
                pageConfigSnapshotService, queryItemService, moduleMetadataFieldService, recordAttachmentService,
                duplicateCheckService, null);
    }

    public DynamicRecordWebController(DynamicRecordService recordService,
                                      ActiveTenantVerifier activeTenantVerifier,
                                      CodeBusinessPreviewService codeBusinessPreviewService,
                                      ReferenceRecordGenerationFacade referenceRecordGenerationFacade,
                                      PlatformPageConfigSnapshotService pageConfigSnapshotService,
                                      PlatformQueryItemService queryItemService,
                                      ModuleMetadataFieldService moduleMetadataFieldService,
                                      RecordAttachmentService recordAttachmentService,
                                      RecordDuplicateCheckService duplicateCheckService,
                                      PlatformRecordNavigationService navigationService) {
        this.recordService = recordService;
        this.activeTenantVerifier = activeTenantVerifier;
        this.codeBusinessPreviewService = codeBusinessPreviewService;
        this.referenceRecordGenerationFacade = referenceRecordGenerationFacade;
        this.pageConfigSnapshotService = pageConfigSnapshotService;
        this.queryItemService = queryItemService;
        this.moduleMetadataFieldService = moduleMetadataFieldService;
        this.recordAttachmentService = recordAttachmentService;
        this.duplicateCheckService = duplicateCheckService;
        this.navigationService = navigationService;
    }

    @Override
    public DynamicEntityOperations service() {
        return recordService.mainEntity(DynamicWebRequest.moduleAlias());
    }

    @Override
    public <T> T webScope(Supplier<T> action) {
        return tenantScope(DynamicWebRequest.moduleAlias(), action);
    }

    @Override
    public Criteria queryCriteria(WebQueryRequest request) {
        Criteria templateCriteria = Criteria.of();
        if (request != null && hasText(request.queryTemplateId())) {
            requireLowCodeQueryServices();
            validateQueryTemplateBelongsToModule(DynamicWebRequest.moduleAlias(), request.queryTemplateId());
            templateCriteria = queryItemService.compile(request.queryTemplateId(), request.externalQueryValues());
        }
        if (request == null || request.conditions().isEmpty()) {
            return templateCriteria;
        }
        Criteria manualCriteria = service().queryCriteria(DynamicWebQueryMapper.queryConditions(request.conditions()));
        if (templateCriteria.isEmpty()) {
            return manualCriteria;
        }
        Criteria criteria = Criteria.of();
        criteria.andGroup(templateCriteria.getRoot());
        if (!manualCriteria.isEmpty()) {
            criteria.andGroup(manualCriteria.getRoot());
        }
        return criteria;
    }

    @Override
    public Sort[] querySorts(WebQueryRequest request) {
        if (request == null || request.sorts().isEmpty()) {
            return new Sort[0];
        }
        return DynamicWebQueryMapper.sorts(request.sorts());
    }

    @Override
    public PageResult<DynamicRecord> queryRecords(WebQueryRequest request) {
        PageResult<DynamicRecord> page = CrudWeb.super.queryRecords(request);
        if (request == null || !hasText(request.uiConfigId())) {
            return page;
        }
        Set<String> projectionFields = projectionFields(DynamicWebRequest.moduleAlias(), request);
        List<DynamicRecord> records = page.getRecords().stream()
                .map(record -> project(record, projectionFields))
                .toList();
        return PageResult.of(records, page.getTotal(), PageRequest.of(page.getPageNum(), page.getPageSize()));
    }

    @Override
    @PostMapping("/query")
    @ActionEndpoint(PlatformAction.QUERY)
    public WebPageResponse<DynamicRecord> query(@RequestBody(required = false) WebQueryRequest request) {
        return webScope(() -> {
            PageResult<DynamicRecord> page = queryRecords(request);
            PageResult<DynamicRecord> output = WebOutputSupport.page(service(), page, FieldOutputContext.LIST);
            return WebPageResponse.from(output, navigationContext(request, output));
        });
    }

    @Override
    @PostMapping("/insert")
    @ActionEndpoint(PlatformAction.CREATE)
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public DynamicRecord insert(@RequestBody DynamicRecord record) {
        return webScope(() -> {
            DynamicRecord normalized = record == null ? service().newRecord() : record;
            validateUiSave(DynamicWebRequest.moduleAlias(), normalized);
            String id = service().insert(normalized);
            syncAttachmentsIfPresent(DynamicWebRequest.moduleAlias(), id, normalized);
            return WebOutputSupport.record(service(), service().select(id), FieldOutputContext.VIEW);
        });
    }

    @Override
    @PostMapping("/update/{id}")
    @ActionEndpoint(PlatformAction.UPDATE)
    @Transactional
    public DynamicRecord update(@PathVariable String id, @RequestBody DynamicRecord record) {
        return webScope(() -> {
            DynamicRecord normalized = record == null ? service().newRecord() : record;
            normalized.setId(id);
            validateUiSave(DynamicWebRequest.moduleAlias(), normalized);
            requireDataScopeRecord(PlatformAction.UPDATE, id);
            service().update(normalized);
            syncAttachmentsIfPresent(DynamicWebRequest.moduleAlias(), id, normalized);
            return WebOutputSupport.record(service(), selectForAction(PlatformAction.VIEW, id), FieldOutputContext.VIEW);
        });
    }

    @PostMapping("/view/{id}/attachments/query")
    @ActionEndpoint(PlatformAction.VIEW)
    public List<RecordAttachment> queryAttachments(@PathVariable String id) {
        return webScope(() -> {
            requireAttachmentService();
            requireDataScopeRecord(PlatformAction.VIEW, id);
            return recordAttachmentService.listByRecord(DynamicWebRequest.moduleAlias(), id);
        });
    }

    @PostMapping("/view/{id}/attachments/add")
    @ActionEndpoint(PlatformAction.UPDATE)
    public List<RecordAttachment> addAttachment(@PathVariable String id,
                                                @RequestBody RecordAttachmentCommand command) {
        return webScope(() -> {
            requireAttachmentService();
            requireDataScopeRecord(PlatformAction.UPDATE, id);
            recordAttachmentService.add(DynamicWebRequest.moduleAlias(), id, command);
            return recordAttachmentService.listByRecord(DynamicWebRequest.moduleAlias(), id);
        });
    }

    @PostMapping("/view/{id}/attachments/update/{attachmentId}")
    @ActionEndpoint(PlatformAction.UPDATE)
    public List<RecordAttachment> updateAttachment(@PathVariable String id,
                                                   @PathVariable String attachmentId,
                                                   @RequestBody RecordAttachmentCommand command) {
        return webScope(() -> {
            requireAttachmentService();
            requireDataScopeRecord(PlatformAction.UPDATE, id);
            recordAttachmentService.updateAttachment(DynamicWebRequest.moduleAlias(), id, attachmentId, command);
            return recordAttachmentService.listByRecord(DynamicWebRequest.moduleAlias(), id);
        });
    }

    @PostMapping("/view/{id}/attachments/delete/{attachmentId}")
    @ActionEndpoint(PlatformAction.UPDATE)
    public List<RecordAttachment> deleteAttachment(@PathVariable String id,
                                                   @PathVariable String attachmentId) {
        return webScope(() -> {
            requireAttachmentService();
            requireDataScopeRecord(PlatformAction.UPDATE, id);
            return recordAttachmentService.deleteAttachment(DynamicWebRequest.moduleAlias(), id, attachmentId);
        });
    }

    private void syncAttachmentsIfPresent(String moduleAlias, String recordId, DynamicRecord record) {
        if (!record.mutationMetadata().containsKey("attachments")) {
            return;
        }
        requireAttachmentService();
        recordAttachmentService.replaceRecordAttachments(moduleAlias, recordId,
                attachmentCommands(record.mutationMetadata().get("attachments")));
    }

    private List<RecordAttachmentCommand> attachmentCommands(Object value) {
        if (value == null) {
            return null;
        }
        if (!(value instanceof List<?> values)) {
            throw new PlatformException("dynamic record attachments must be array");
        }
        List<RecordAttachmentCommand> commands = new ArrayList<>();
        for (Object item : values) {
            if (!(item instanceof Map<?, ?> map)) {
                throw new PlatformException("dynamic record attachment must be object");
            }
            commands.add(new RecordAttachmentCommand(
                    text(map.get("id")),
                    text(map.get("fileId")),
                    text(map.get("displayName")),
                    intValue(map.get("sort")),
                    text(map.get("remark"))
            ));
        }
        return commands;
    }

    private void requireAttachmentService() {
        if (recordAttachmentService == null) {
            throw new PlatformException("record attachment service is not configured");
        }
    }

    private DynamicRecord selectForAction(PlatformAction action, String id) {
        Object operations = service();
        if (operations instanceof DataScopeAbility<?> dataScopeAbility) {
            return selectFromDataScope(dataScopeAbility, action, id);
        }
        return service().select(id);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private DynamicRecord selectFromDataScope(DataScopeAbility dataScopeAbility, PlatformAction action, String id) {
        return (DynamicRecord) dataScopeAbility.selectForAction(action, id);
    }

    private void requireDataScopeRecord(PlatformAction action, String id) {
        Object operations = service();
        if (operations instanceof DataScopeAbility<?> dataScopeAbility) {
            requireRecordScope(dataScopeAbility, actionPolicy(action), id);
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void requireRecordScope(DataScopeAbility dataScopeAbility, ActionExecutionPolicy policy, String id) {
        dataScopeAbility.requireRecordScope(policy, java.util.List.of(id));
    }

    private ActionExecutionPolicy actionPolicy(PlatformAction fallback) {
        return ActionExecutionContextHolder.current()
                .filter(context -> context.moduleAlias().equals(webScopeName()))
                .map(ActionExecutionContext::actionPolicy)
                .orElseGet(fallback::executionPolicy);
    }

    private Set<String> projectionFields(String moduleAlias, WebQueryRequest request) {
        requireLowCodePageServices();
        PlatformPageConfigSnapshot snapshot = pageConfigSnapshotService.snapshot(moduleAlias);
        PlatformUiConfig uiConfig = snapshot.uiConfigs().stream()
                .filter(config -> Objects.equals(config.getId(), request.uiConfigId()))
                .findFirst()
                .orElseThrow(() -> new PlatformException("UI config is not published in module snapshot: "
                        + request.uiConfigId()));
        Set<String> fields = new LinkedHashSet<>();
        for (PlatformUiConfigField field : snapshot.uiFields()) {
            if (!Objects.equals(field.getUiConfigId(), uiConfig.getId())
                    || !Boolean.TRUE.equals(field.getVisible())) {
                continue;
            }
            ResolvedModuleMetadataField resolved = moduleMetadataFieldService.resolve(field.getModuleMetadataFieldId());
            if (resolved.relationRole() != RelationRole.MAIN) {
                throw new PlatformException("List UI config only supports main relation fields: "
                        + field.getModuleMetadataFieldId());
            }
            fields.add(resolved.fieldName());
        }
        return fields;
    }

    private void validateUiSave(String moduleAlias, DynamicRecord record) {
        Object uiConfigIdValue = record.mutationMetadata().get("uiConfigId");
        if (!(uiConfigIdValue instanceof String uiConfigId) || !hasText(uiConfigId)) {
            return;
        }
        requireLowCodeQueryServices();
        PlatformPageConfigSnapshot snapshot = pageConfigSnapshotService.snapshot(moduleAlias);
        PlatformUiConfig uiConfig = snapshot.uiConfigs().stream()
                .filter(config -> Objects.equals(config.getId(), uiConfigId))
                .findFirst()
                .orElseThrow(() -> new PlatformException("UI config is not published in module snapshot: "
                        + uiConfigId));
        Map<String, FieldDefinition> fields = record.getEntity().fields().stream()
                .collect(java.util.stream.Collectors.toMap(FieldDefinition::fieldName, field -> field));
        for (PlatformUiConfigField uiField : snapshot.uiFields()) {
            if (!Objects.equals(uiField.getUiConfigId(), uiConfig.getId())
                    || !Boolean.TRUE.equals(uiField.getVisible())) {
                continue;
            }
            ResolvedModuleMetadataField resolved = moduleMetadataFieldService.resolve(uiField.getModuleMetadataFieldId());
            if (resolved.relationRole() != RelationRole.MAIN) {
                throw new PlatformException("Form UI config only supports main relation fields for save validation: "
                        + uiField.getModuleMetadataFieldId());
            }
            FieldDefinition field = fields.get(resolved.fieldName());
            if (field == null) {
                continue;
            }
            validateUiReadOnly(record, uiField, field);
            validateUiRequired(record, uiField, field);
        }
    }

    private void validateUiReadOnly(DynamicRecord record, PlatformUiConfigField uiField, FieldDefinition field) {
        if (Boolean.TRUE.equals(uiField.getReadOnly()) && record.isExplicitlySet(field.fieldName())) {
            throw new PlatformException("UI read-only field cannot be saved: " + field.fieldName());
        }
    }

    private void validateUiRequired(DynamicRecord record, PlatformUiConfigField uiField, FieldDefinition field) {
        boolean required = Boolean.TRUE.equals(uiField.getRequiredOverride()) || field.isRequired();
        if (!required) {
            return;
        }
        Object value = record.getValues().get(field.fieldName());
        if (value == null || value instanceof String text && text.isBlank()) {
            throw new PlatformException("UI required field is missing: " + field.fieldName());
        }
    }

    private void validateQueryTemplateBelongsToModule(String moduleAlias, String queryTemplateId) {
        if (!hasText(queryTemplateId)) {
            return;
        }
        requireLowCodePageServices();
        PlatformPageConfigSnapshot snapshot = pageConfigSnapshotService.snapshot(moduleAlias);
        PlatformQueryTemplate template = snapshot.queryTemplates().stream()
                .filter(item -> Objects.equals(item.getId(), queryTemplateId))
                .findFirst()
                .orElseThrow(() -> new PlatformException("Query template is not enabled in module snapshot: "
                        + queryTemplateId));
        if (!Objects.equals(template.getModuleAlias(), moduleAlias)) {
            throw new PlatformException("Query template must belong to module: " + moduleAlias);
        }
    }

    private DynamicRecord project(DynamicRecord source, Set<String> fields) {
        DynamicRecord projected = new DynamicRecord(source.getEntity());
        projected.setId(source.getId());
        projected.setTenantId(source.getTenantId());
        projected.setVersion(source.getVersion());
        for (String field : fields) {
            Map<String, Object> values = source.getValues();
            if (values.containsKey(field)) {
                projected.setValue(field, values.get(field));
            }
        }
        return projected;
    }

    private void requireLowCodePageServices() {
        if (pageConfigSnapshotService == null || moduleMetadataFieldService == null) {
            throw new PlatformException("dynamic low-code page services are not configured");
        }
    }

    private void requireLowCodeQueryServices() {
        requireLowCodePageServices();
        if (queryItemService == null) {
            throw new PlatformException("dynamic low-code query services are not configured");
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String text(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return text.isBlank() ? null : text;
    }

    private Integer intValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value).trim());
        } catch (NumberFormatException ex) {
            throw new PlatformException("dynamic record attachment sort must be number", ex);
        }
    }

    @Override
    @PostMapping("/sort/{id}")
    @ActionEndpoint(PlatformAction.SORT)
    public WebCountResponse sort(@PathVariable String id,
                                 @RequestBody(required = false) TreeSortWebRequest request) {
        return webScope(() -> {
            TreeSortWebRequest normalized = request == null ? new TreeSortWebRequest(null, null, null) : request;
            DynamicEntityOperations operations = service();
            Set<String> capabilities = operations.describe().capabilities();
            if (capabilities.contains(EntityCapability.TREE.name())) {
                requireSortInput(normalized);
                operations.moveInTree(id, normalized.previousId(), normalized.nextId(), normalized.parentId());
                return new WebCountResponse(1);
            }
            if (!capabilities.contains(EntityCapability.SORT.name())) {
                throw new PlatformException("dynamic entity does not support capability: SORT");
            }
            if (normalized.parentId() != null && !normalized.parentId().isBlank()) {
                throw new IllegalArgumentException("sort parentId requires TREE capability");
            }
            if (normalized.previousId() != null && !normalized.previousId().isBlank()) {
                operations.moveAfter(id, normalized.previousId());
                return new WebCountResponse(1);
            }
            if (normalized.nextId() != null && !normalized.nextId().isBlank()) {
                operations.moveBefore(id, normalized.nextId());
                return new WebCountResponse(1);
            }
            throw new IllegalArgumentException("sort requires previousId or nextId");
        });
    }

    @GetMapping("/describe")
    public DynamicModuleDescriptor describeModule(@PathVariable String moduleAlias) {
        return tenantScope(moduleAlias, () -> permissionScopedDescriptor(moduleAlias));
    }

    @GetMapping("/openapi")
    public DynamicOpenApiDocument openApi(@PathVariable String moduleAlias) {
        return tenantScope(moduleAlias, () -> openApiGenerator.generate(
                permissionScopedDescriptor(moduleAlias),
                action -> recordService.actionAuthorizationAvailability(
                        moduleAlias, action.code(), Set.of()).available()));
    }

    @GetMapping("/navigation/{sessionId}/{recordId}")
    @ActionEndpoint(PlatformAction.VIEW)
    public PlatformRecordNavigationMove navigation(@PathVariable String sessionId,
                                                   @PathVariable String recordId) {
        return webScope(() -> {
            requireNavigationService();
            PlatformRecordNavigationMove move = navigationService.move(DynamicWebRequest.moduleAlias(), sessionId, recordId);
            requireNavigationViewScope(move.currentRecordId());
            requireNavigationViewScope(move.previousRecordId());
            requireNavigationViewScope(move.nextRecordId());
            return move;
        });
    }

    @PostMapping("/code/preview")
    @ActionEndpoint(PlatformAction.CREATE)
    public List<CodeBusinessPreviewItem> previewCode(@PathVariable String moduleAlias,
                                                     @RequestBody(required = false) DynamicRecord record) {
        return tenantScope(moduleAlias, () -> {
            if (codeBusinessPreviewService == null) {
                throw new PlatformException("code business preview service is not configured");
            }
            DynamicRecord normalized = record == null ? service().newRecord() : record;
            return codeBusinessPreviewService.preview(
                    moduleAlias,
                    mainEntityAlias(moduleAlias),
                    normalized.getValues(),
                    resolveOrganizationId(normalized),
                    null,
                    null
            );
        });
    }

    @Override
    public List<DynamicActionDescriptor> listActions() {
        String moduleAlias = DynamicWebRequest.moduleAlias();
        return recordService.actions(moduleAlias).stream()
                .filter(action -> recordService.actionAuthorizationAvailability(moduleAlias, action.code(), Set.of()).available())
                .toList();
    }

    @Override
    public List<DynamicWebActionAvailabilityResponse> listRecordActions(String recordId) {
        String moduleAlias = DynamicWebRequest.moduleAlias();
        String entityAlias = mainEntityAlias(moduleAlias);
        DynamicRecord record = recordService.select(moduleAlias, entityAlias, recordId);
        if (record == null) {
            throw new IllegalArgumentException("dynamic record does not exist: " + recordId);
        }
        return recordService.actions(moduleAlias).stream()
                .filter(DynamicRecordWebController::isRecordAction)
                .filter(action -> recordService.actionAuthorizationAvailability(
                        moduleAlias, entityAlias, action.code(), Set.of(recordId)).available())
                .map(action -> DynamicWebActionAvailabilityResponse.from(action,
                        recordService.actionAvailability(moduleAlias, action.code(), record)))
                .toList();
    }

    @Override
    public DynamicWebActionExecutionResponse executeListAction(String actionCode, DynamicWebActionRequest request) {
        String moduleAlias = DynamicWebRequest.moduleAlias();
        requireActionLevel(moduleAlias, actionCode, Set.of(EntityActionLevel.LIST, EntityActionLevel.ANY),
                "dynamic action does not support list path: ");
        return executeAction(moduleAlias, actionCode, null, request);
    }

    @Override
    public DynamicWebActionExecutionResponse executeBatchAction(String actionCode, DynamicWebActionRequest request) {
        String moduleAlias = DynamicWebRequest.moduleAlias();
        DynamicWebActionRequest normalized = request == null ? DynamicWebActionRequest.empty() : request;
        if (normalized.ids().isEmpty()) {
            throw new IllegalArgumentException("batch action requires ids");
        }
        requireActionLevel(moduleAlias, actionCode, Set.of(EntityActionLevel.BATCH, EntityActionLevel.ANY),
                "dynamic action does not support batch path: ");
        return executeAction(moduleAlias, actionCode, null, normalized);
    }

    @Override
    public DynamicWebActionExecutionResponse executeRecordAction(String actionCode,
                                                                 String recordId,
                                                                 DynamicWebActionRequest request) {
        String moduleAlias = DynamicWebRequest.moduleAlias();
        requireActionLevel(moduleAlias, actionCode, Set.of(EntityActionLevel.RECORD, EntityActionLevel.ANY),
                "dynamic action does not support record path: ");
        return executeAction(moduleAlias, actionCode, recordId, request);
    }

    @PostMapping("/{actionCode}/duplicate/check")
    public RecordDuplicateCheckResult checkDuplicate(@PathVariable String actionCode,
                                                     @RequestBody(required = false) DynamicWebDuplicateCheckRequest request) {
        String moduleAlias = DynamicWebRequest.moduleAlias();
        requireActionLevel(moduleAlias, actionCode, Set.of(EntityActionLevel.RECORD, EntityActionLevel.ANY),
                "dynamic duplicate action must be record action: ");
        return webScope(() -> {
            requireDuplicateCheckService();
            DynamicWebDuplicateCheckRequest normalized = request == null
                    ? DynamicWebDuplicateCheckRequest.empty()
                    : request;
            DynamicActionAvailability availability = recordService.actionAuthorizationAvailability(
                    moduleAlias, actionCode, duplicateRecordIds(normalized));
            if (!availability.available()) {
                throw new PlatformException(hasText(availability.message())
                        ? availability.message()
                        : "dynamic duplicate action is not available: " + actionCode);
            }
            return duplicateCheckService.check(moduleAlias, actionCode, normalized.recordId(), normalized.values());
        });
    }

    private Set<String> duplicateRecordIds(DynamicWebDuplicateCheckRequest request) {
        return request.recordId() == null || request.recordId().isBlank()
                ? Set.of()
                : Set.of(request.recordId().trim());
    }

    @Override
    @PostMapping("/references/{fieldName}/resolve")
    @ActionEndpoint(PlatformAction.REFERENCE)
    public DynamicReferenceResolveResponse reference(@PathVariable String fieldName,
                                                     @RequestBody(required = false) DynamicWebReferenceRequest request) {
        return ReferenceWeb.super.reference(fieldName, request);
    }

    @PostMapping("/references/{fieldName}/generate")
    @ActionEndpoint(PlatformAction.REFERENCE)
    public RecordGenerationResult generateFromReference(@PathVariable String fieldName,
                                                        @RequestBody(required = false) DynamicWebReferenceGenerationRequest request) {
        return webScope(() -> {
            DynamicWebReferenceGenerationRequest normalized = request == null
                    ? new DynamicWebReferenceGenerationRequest(null)
                    : request;
            return referenceGenerationFacade().generateFromReference(
                    DynamicWebRequest.moduleAlias(),
                    mainEntityAlias(DynamicWebRequest.moduleAlias()),
                    fieldName,
                    normalized.sourceRecordId()
            );
        });
    }

    @PostMapping("/generation/confirm")
    @ActionEndpoint(PlatformAction.CREATE)
    public RecordGenerationCommitResult confirmGeneratedDraft(@RequestBody(required = false) DynamicWebGenerationConfirmRequest request) {
        return webScope(() -> {
            DynamicWebGenerationConfirmRequest normalized = request == null
                    ? new DynamicWebGenerationConfirmRequest(null, null, null, null)
                    : request;
            String targetModuleAlias = requireText(normalized.targetModuleAlias(), "targetModuleAlias");
            String targetEntityAlias = requireText(normalized.targetEntityAlias(), "targetEntityAlias");
            requirePathModule(targetModuleAlias);
            DynamicRecord draft = record(targetModuleAlias, targetEntityAlias, normalized.record());
            String id = referenceGenerationFacade().confirmDraft(new RecordGenerationDraft(
                    targetModuleAlias,
                    targetEntityAlias,
                    draft,
                    normalized.originContext()
            ));
            return new RecordGenerationCommitResult(
                    normalized.originContext() == null ? null : normalized.originContext().generationRuleId(),
                    normalized.originContext() == null ? null : normalized.originContext().batchId(),
                    targetModuleAlias,
                    List.of(id)
            );
        });
    }

    private DynamicWebActionExecutionResponse executeAction(String moduleAlias,
                                                            String actionCode,
                                                            String pathRecordId,
                                                            DynamicWebActionRequest request) {
        String entityAlias = recordService.actionEntityAlias(moduleAlias, actionCode);
        return DynamicWebActionExecutionResponse.from(recordService.executeAction(
                moduleAlias, actionCode, actionRequest(moduleAlias, entityAlias, pathRecordId, request)));
    }

    @Override
    public DynamicReferenceResolveResponse resolveReference(String fieldName, DynamicWebReferenceRequest request) {
        String moduleAlias = DynamicWebRequest.moduleAlias();
        String entityAlias = mainEntityAlias(moduleAlias);
        DynamicWebReferenceRequest normalized = request == null ? DynamicWebReferenceRequest.empty() : request;
        return recordService.resolveFieldReference(moduleAlias, entityAlias, fieldName, new DynamicReferenceResolveRequest(
                normalized.mode(),
                normalized.matchMode(),
                normalized.fuzzy(),
                normalized.values(),
                criteria(moduleAlias, entityAlias, normalized.conditions()),
                DynamicWebQueryMapper.page(normalized.page()),
                normalized.includeProjections(),
                normalized.formValues()
        ));
    }

    private ReferenceRecordGenerationFacade referenceGenerationFacade() {
        if (referenceRecordGenerationFacade == null) {
            throw new PlatformException("reference record generation facade is not configured");
        }
        return referenceRecordGenerationFacade;
    }

    private void requireDuplicateCheckService() {
        if (duplicateCheckService == null) {
            throw new PlatformException("record duplicate check service is not configured");
        }
    }

    private void requireNavigationService() {
        if (navigationService == null) {
            throw new PlatformException("record navigation service is not configured");
        }
    }

    private void requireNavigationViewScope(String recordId) {
        if (recordId != null && !recordId.isBlank()) {
            DynamicRecord visible = recordService.select(
                    DynamicWebRequest.moduleAlias(),
                    mainEntityAlias(DynamicWebRequest.moduleAlias()),
                    recordId);
            if (visible == null) {
                throw new PlatformException("record navigation record is not visible: " + recordId);
            }
        }
    }

    private PlatformRecordNavigationContext navigationContext(WebQueryRequest request,
                                                             PageResult<DynamicRecord> page) {
        if (request == null || !request.navigationSessionEnabled() || page.getRecords().isEmpty()) {
            return null;
        }
        requireNavigationService();
        return navigationService.createCurrentUserSession(
                DynamicWebRequest.moduleAlias(),
                mainEntityAlias(DynamicWebRequest.moduleAlias()),
                page.getRecords().stream().map(DynamicRecord::getId).toList(),
                page.getPageNum(),
                page.getPageSize(),
                page.getTotal()
        );
    }

    private String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new PlatformException("dynamic generation confirm requires " + fieldName);
        }
        return value.trim();
    }

    private void requirePathModule(String targetModuleAlias) {
        String pathModuleAlias = DynamicWebRequest.moduleAlias();
        if (!pathModuleAlias.equals(targetModuleAlias)) {
            throw new PlatformException("dynamic generation confirm targetModuleAlias mismatch: "
                    + targetModuleAlias + " != " + pathModuleAlias);
        }
    }

    @ExceptionHandler({IllegalArgumentException.class, ModuleDefinitionException.class, PlatformException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public DynamicWebError handleBadRequest(RuntimeException exception) {
        return DynamicWebError.badRequest(exception.getMessage());
    }

    @ExceptionHandler(HttpMessageConversionException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public DynamicWebError handleMessageConversion(HttpMessageConversionException exception) {
        return DynamicWebError.badRequest(rootMessage(exception));
    }

    @ExceptionHandler(DynamicActionExecutionException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public DynamicWebActionError handleActionFailure(DynamicActionExecutionException exception) {
        return DynamicWebActionError.from(exception);
    }

    @ExceptionHandler(OptimisticLockException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public DynamicWebError handleOptimisticLock(OptimisticLockException exception) {
        return DynamicWebError.conflict(exception.getMessage());
    }

    private DynamicRecord record(String moduleAlias, String entityAlias, DynamicRecordPayload payload) {
        DynamicRecord record = recordService.newRecord(moduleAlias, entityAlias);
        DynamicRecordPayload normalized = payload == null ? DynamicRecordPayload.empty() : payload;
        if (normalized.id() != null && !normalized.id().isBlank()) {
            record.setId(normalized.id());
        }
        if (normalized.version() != null) {
            record.setVersion(normalized.version());
        }
        normalized.values().forEach(record::setValue);
        normalized.children().forEach((relationCode, rows) -> {
            if (rows == null) {
                throw new PlatformException("dynamic child relation must be array: " + relationCode);
            }
            String childEntityAlias = childEntityAlias(moduleAlias, entityAlias, relationCode);
            record.setChildren(
                    relationCode,
                    rows.stream()
                            .map(row -> record(moduleAlias, childEntityAlias, row))
                            .toList()
            );
        });
        return record;
    }

    private String childEntityAlias(String moduleAlias, String parentEntityAlias, String relationCode) {
        return recordService.relations(moduleAlias).stream()
                .filter(relation -> relation.parentEntityAlias().equals(parentEntityAlias))
                .filter(relation -> relation.code().equals(relationCode))
                .map(DynamicRelationDescriptor::childEntityAlias)
                .findFirst()
                .orElseThrow(() -> new PlatformException("unknown dynamic child relation: " + relationCode));
    }

    private String mainEntityAlias(String moduleAlias) {
        return recordService.mainEntityAlias(moduleAlias);
    }

    private String resolveOrganizationId(DynamicRecord record) {
        if (record != null && record.getAuthOrganizationId() != null && !record.getAuthOrganizationId().isBlank()) {
            return record.getAuthOrganizationId();
        }
        return CurrentUserContext.currentUser()
                .map(CurrentUser::organizationId)
                .orElse(null);
    }

    private DynamicModuleDescriptor permissionScopedDescriptor(String moduleAlias) {
        DynamicModuleDescriptor descriptor = recordService.describe(moduleAlias);
        return new DynamicModuleDescriptor(
                descriptor.moduleAlias(),
                descriptor.title(),
                descriptor.mainEntityAlias(),
                visibleModuleActions(moduleAlias, descriptor.actions()),
                descriptor.entities().stream()
                        .map(entity -> new DynamicEntityDescriptor(
                                entity.entityAlias(),
                                entity.title(),
                                entity.capabilities(),
                                entity.fields(),
                                entity.formulaRules(),
                                visibleEntityActions(moduleAlias, entity.entityAlias(), entity.actions()),
                                entity.views(),
                                entity.associationViews()
                        ))
                        .toList(),
                descriptor.relations(),
                descriptor.references(),
                descriptor.associationViews()
        );
    }

    private List<DynamicActionDescriptor> visibleModuleActions(String moduleAlias,
                                                               List<DynamicActionDescriptor> actions) {
        return actions.stream()
                .filter(action -> recordService.actionAuthorizationAvailability(
                        moduleAlias, action.code(), Set.of()).available())
                .toList();
    }

    private List<DynamicActionDescriptor> visibleEntityActions(String moduleAlias,
                                                               String entityAlias,
                                                               List<DynamicActionDescriptor> actions) {
        return actions.stream()
                .filter(action -> recordService.actionAuthorizationAvailability(
                        moduleAlias, entityAlias, action.code(), Set.of()).available())
                .toList();
    }

    private void requireSortInput(TreeSortWebRequest request) {
        if ((request.previousId() == null || request.previousId().isBlank())
                && (request.nextId() == null || request.nextId().isBlank())
                && (request.parentId() == null || request.parentId().isBlank())) {
            throw new IllegalArgumentException("tree sort requires previousId, nextId, or parentId");
        }
    }

    private <T> T tenantScope(String moduleAlias, Supplier<T> action) {
        String tenantId = TenantContext.currentTenantId()
                .orElseThrow(() -> new PlatformException(moduleAlias + " requires tenant context"));
        activeTenantVerifier.verifyActiveTenant(tenantId);
        return action.get();
    }

    private Criteria criteria(String moduleAlias, String entityAlias, List<WebQueryCondition> conditions) {
        List<DynamicQueryCondition> queryConditions = DynamicWebQueryMapper.queryConditions(conditions);
        if (queryConditions.isEmpty()) {
            return Criteria.of();
        }
        return recordService.queryCriteria(moduleAlias, entityAlias, queryConditions);
    }

    private DynamicActionExecutionRequest actionRequest(String moduleAlias,
                                                        String entityAlias,
                                                        String pathRecordId,
                                                        DynamicWebActionRequest request) {
        DynamicWebActionRequest normalized = request == null ? DynamicWebActionRequest.empty() : request;
        String recordId = resolveActionRecordId(pathRecordId, normalized.recordId());
        DynamicActionExecutionRequest actionRequest = DynamicActionExecutionRequest.empty()
                .withRecordId(recordId)
                .withIds(normalized.ids())
                .withOrderedIds(normalized.orderedIds())
                .withBeforeId(normalized.beforeId())
                .withAfterId(normalized.afterId())
                .withParentId(normalized.parentId())
                .withFieldNames(normalized.fieldNames())
                .withQueryConditions(DynamicWebQueryMapper.queryConditions(normalized.conditions()))
                .withPayload(normalized.payload());
        if (normalized.record() != null && entityAlias != null) {
            actionRequest = actionRequest.withRecord(actionRecord(moduleAlias, entityAlias, recordId, normalized.record()));
        }
        if (!normalized.conditions().isEmpty() && entityAlias != null) {
            actionRequest = actionRequest.withCriteria(criteria(moduleAlias, entityAlias, normalized.conditions()));
        }
        if (normalized.page() != null) {
            actionRequest = actionRequest.withPageRequest(DynamicWebQueryMapper.page(normalized.page()));
        }
        if (!normalized.sorts().isEmpty()) {
            actionRequest = actionRequest.withSorts(List.of(DynamicWebQueryMapper.sorts(normalized.sorts())));
        }
        return actionRequest;
    }

    private void requireActionLevel(String moduleAlias,
                                    String actionCode,
                                    Set<EntityActionLevel> allowed,
        String messagePrefix) {
        rejectReservedActionPath(actionCode);
        EntityActionLevel level = recordService.action(moduleAlias, actionCode).actionLevel();
        if (!allowed.contains(level)) {
            throw new IllegalArgumentException(messagePrefix + actionCode);
        }
    }

    private void rejectReservedActionPath(String actionCode) {
        if (PlatformWebPathRules.isReservedWebActionCode(actionCode)) {
            throw new IllegalArgumentException("dynamic action path is reserved: " + actionCode);
        }
    }

    private String resolveActionRecordId(String pathRecordId, String bodyRecordId) {
        if (pathRecordId == null || pathRecordId.isBlank()) {
            return bodyRecordId;
        }
        if (bodyRecordId != null && !bodyRecordId.isBlank() && !pathRecordId.equals(bodyRecordId)) {
            throw new IllegalArgumentException("action path recordId must match request recordId");
        }
        return pathRecordId;
    }

    private DynamicRecord actionRecord(String moduleAlias,
                                       String entityAlias,
                                       String recordId,
                                       DynamicRecordPayload payload) {
        DynamicRecord record = record(moduleAlias, entityAlias, payload);
        if (recordId == null || recordId.isBlank()) {
            return record;
        }
        if (record.getId() != null && !record.getId().isBlank() && !recordId.equals(record.getId())) {
            throw new IllegalArgumentException("action request recordId must match record.id");
        }
        record.setId(recordId);
        return record;
    }

    private String rootMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return current.getMessage();
    }

    private static boolean isRecordAction(DynamicActionDescriptor action) {
        return action.actionLevel() == EntityActionLevel.RECORD || action.actionLevel() == EntityActionLevel.ANY;
    }
}
