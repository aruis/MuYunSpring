package net.ximatai.muyun.spring.platform.code;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.database.core.orm.Sort;
import net.ximatai.muyun.spring.ability.AbstractAbilityService;
import net.ximatai.muyun.spring.ability.BaseDao;
import net.ximatai.muyun.spring.ability.EnableAbility;
import net.ximatai.muyun.spring.ability.SoftDeleteAbility;
import net.ximatai.muyun.spring.ability.SortAbility;
import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.common.platform.OrganizationHierarchyService;
import net.ximatai.muyun.spring.common.util.PlatformNameRules;
import net.ximatai.muyun.spring.platform.metadata.Metadata;
import net.ximatai.muyun.spring.platform.metadata.MetadataField;
import net.ximatai.muyun.spring.platform.metadata.MetadataFieldService;
import net.ximatai.muyun.spring.platform.metadata.MetadataService;
import net.ximatai.muyun.spring.platform.metadata.ModuleMetadataRelation;
import net.ximatai.muyun.spring.platform.metadata.ModuleMetadataRelationService;
import net.ximatai.muyun.spring.platform.module.PlatformModuleService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@Service
public class CodeRuleService extends AbstractAbilityService<CodeRule> implements
        SoftDeleteAbility<CodeRule>,
        EnableAbility<CodeRule>,
        SortAbility<CodeRule> {
    public static final String MODULE_ALIAS = "platform.code_rule";
    private static final PageRequest ALL = new PageRequest(0, Integer.MAX_VALUE);

    private final CodeRuleSegmentService segmentService;
    private final CodeSequencePolicyService sequencePolicyService;
    private final CodeValueMappingService mappingService;
    private final Optional<PlatformModuleService> moduleService;
    private final Optional<ModuleMetadataRelationService> relationService;
    private final Optional<MetadataService> metadataService;
    private final Optional<MetadataFieldService> fieldService;
    private final Optional<OrganizationHierarchyService> organizationHierarchyService;

    public CodeRuleService(BaseDao<CodeRule, String> ruleDao,
                           CodeRuleSegmentService segmentService,
                           CodeSequencePolicyService sequencePolicyService,
                           CodeValueMappingService mappingService,
                           Optional<PlatformModuleService> moduleService,
                           Optional<ModuleMetadataRelationService> relationService,
                           Optional<MetadataService> metadataService,
                           Optional<MetadataFieldService> fieldService,
                           Optional<OrganizationHierarchyService> organizationHierarchyService) {
        super(MODULE_ALIAS, CodeRule.class, ruleDao);
        this.segmentService = Objects.requireNonNull(segmentService, "segmentService must not be null");
        this.sequencePolicyService = Objects.requireNonNull(sequencePolicyService, "sequencePolicyService must not be null");
        this.mappingService = Objects.requireNonNull(mappingService, "mappingService must not be null");
        this.moduleService = moduleService == null ? Optional.empty() : moduleService;
        this.relationService = relationService == null ? Optional.empty() : relationService;
        this.metadataService = metadataService == null ? Optional.empty() : metadataService;
        this.fieldService = fieldService == null ? Optional.empty() : fieldService;
        this.organizationHierarchyService = organizationHierarchyService == null ? Optional.empty() : organizationHierarchyService;
    }

    public CodeRuleService(BaseDao<CodeRule, String> ruleDao,
                           CodeRuleSegmentService segmentService,
                           CodeSequencePolicyService sequencePolicyService,
                           CodeValueMappingService mappingService) {
        this(ruleDao, segmentService, sequencePolicyService, mappingService,
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
    }

    @Transactional
    public CodeRule saveRuleTree(CodeRule rule) {
        if (rule == null) {
            throw new PlatformException("Code rule tree must not be null");
        }
        List<CodeRuleSegment> segments = rule.getSegments() == null ? List.of() : new ArrayList<>(rule.getSegments());
        CodeSequencePolicy sequencePolicy = rule.getSequencePolicy();
        validateRuleTreeSemantics(rule, segments, sequencePolicy);
        boolean updating = rule.getId() != null && select(rule.getId()) != null;
        if (updating) {
            update(rule);
        } else {
            insert(rule);
        }
        replaceSequencePolicy(rule, sequencePolicy);
        replaceSegments(rule, segments);
        return viewRuleTree(rule.getId());
    }

    public CodeRule viewRuleTree(String ruleId) {
        CodeRule rule = select(ruleId);
        if (rule == null) {
            return null;
        }
        List<CodeRuleSegment> segments = segmentService.selectByRuleId(ruleId);
        for (CodeRuleSegment segment : segments) {
            segment.setMappings(mappingService.selectBySegmentId(segment.getId()));
        }
        rule.setSegments(segments);
        rule.setSequencePolicy(sequencePolicyService.selectByRuleId(ruleId));
        return rule;
    }

    public List<ResolvedCodeRule> resolveRules(ResolveCodeRuleCommand command) {
        validateResolveCommand(command);
        LocalDateTime effectiveAt = command.at() == null ? LocalDateTime.now() : command.at();
        List<CodeRule> candidates = list(Criteria.of()
                        .eq("moduleAlias", PlatformNameRules.requireModuleAlias(command.moduleAlias()))
                        .eq("entityAlias", PlatformNameRules.requireIdentifier(command.entityAlias(), "entityAlias")),
                ALL, Sort.asc("sortOrder")).stream()
                .filter(rule -> Boolean.TRUE.equals(rule.getEnabled()))
                .filter(rule -> isEffective(rule, effectiveAt))
                .filter(rule -> matchesRequestedField(rule, command.metadataFieldId(), command.fieldName()))
                .map(rule -> viewRuleTree(rule.getId()))
                .toList();
        if (candidates.isEmpty()) {
            return List.of();
        }
        LinkedHashMap<String, List<CodeRule>> byField = new LinkedHashMap<>();
        for (CodeRule candidate : candidates) {
            byField.computeIfAbsent(targetFieldKey(candidate), ignored -> new ArrayList<>()).add(candidate);
        }

        List<ResolvedCodeRule> resolved = new ArrayList<>();
        for (List<CodeRule> fieldCandidates : byField.values()) {
            ResolvedCodeRule rule = resolveForField(fieldCandidates, command.organizationId());
            if (rule != null) {
                resolved.add(rule);
            }
        }
        long primaryCount = resolved.stream()
                .filter(item -> item.rule().getFieldRole() == CodeFieldRole.PRIMARY)
                .map(item -> targetFieldKey(item.rule()))
                .distinct()
                .count();
        if (primaryCount > 1) {
            throw new PlatformException("Only one PRIMARY code field can be resolved for the same business object");
        }
        return resolved;
    }

    public ResolvedCodeRule resolveRule(ResolveCodeRuleCommand command) {
        List<ResolvedCodeRule> resolved = resolveRules(command);
        return resolved.stream()
                .filter(item -> item.rule().getFieldRole() == CodeFieldRole.PRIMARY)
                .findFirst()
                .orElse(resolved.isEmpty() ? null : resolved.getFirst());
    }

    @Override
    public void beforeInsert(CodeRule rule) {
        normalizeAndValidate(rule);
    }

    @Override
    public void beforeUpdate(CodeRule rule) {
        normalizeAndValidate(rule);
    }

    @Override
    public Criteria sortScope(CodeRule rule) {
        return Criteria.of()
                .eq("moduleAlias", rule.getModuleAlias())
                .eq("entityAlias", rule.getEntityAlias());
    }

    @Override
    public void validateSortScope(CodeRule left, CodeRule right) {
        if (!Objects.equals(left.getModuleAlias(), right.getModuleAlias())
                || !Objects.equals(left.getEntityAlias(), right.getEntityAlias())) {
            throw new PlatformException("Code rule sort can only move records within the same business object");
        }
    }

    private void replaceSequencePolicy(CodeRule rule, CodeSequencePolicy incoming) {
        CodeSequencePolicy existing = sequencePolicyService.selectByRuleId(rule.getId());
        if (existing != null) {
            sequencePolicyService.delete(existing);
        }
        if (incoming == null) {
            return;
        }
        incoming.setId(null);
        incoming.setRuleId(rule.getId());
        sequencePolicyService.insert(incoming);
    }

    private void replaceSegments(CodeRule rule, List<CodeRuleSegment> incoming) {
        for (CodeRuleSegment existing : segmentService.selectByRuleId(rule.getId())) {
            for (CodeValueMapping mapping : mappingService.selectBySegmentId(existing.getId())) {
                mappingService.delete(mapping);
            }
            segmentService.delete(existing);
        }
        int sort = 1;
        for (CodeRuleSegment segment : incoming) {
            List<CodeValueMapping> mappings = segment.getMappings() == null ? List.of() : new ArrayList<>(segment.getMappings());
            segment.setId(null);
            segment.setRuleId(rule.getId());
            if (segment.getSortOrder() == null) {
                segment.setSortOrder(sort);
            }
            segmentService.insert(segment);
            int mappingSort = 1;
            for (CodeValueMapping mapping : mappings) {
                mapping.setId(null);
                mapping.setSegmentId(segment.getId());
                if (mapping.getSortOrder() == null) {
                    mapping.setSortOrder(mappingSort);
                }
                mappingService.insert(mapping);
                mappingSort++;
            }
            sort++;
        }
    }

    private void normalizeAndValidate(CodeRule rule) {
        rule.setModuleAlias(PlatformNameRules.requireModuleAlias(rule.getModuleAlias()));
        rule.setEntityAlias(PlatformNameRules.requireIdentifier(rule.getEntityAlias(), "entityAlias"));
        if (rule.getMetadataFieldId() != null && !rule.getMetadataFieldId().isBlank()) {
            rule.setMetadataFieldId(PlatformNameRules.requireIdentifier(rule.getMetadataFieldId(), "metadataFieldId"));
        }
        rule.setFieldName(PlatformNameRules.requireFieldName(rule.getFieldName(), "fieldName"));
        if (rule.getTitle() == null || rule.getTitle().isBlank()) {
            rule.setTitle(rule.getFieldName());
        }
        if (rule.getFieldRole() == null) {
            rule.setFieldRole(CodeFieldRole.NORMAL);
        }
        if (rule.getMode() == null) {
            rule.setMode(CodeMode.AUTO);
        }
        if (rule.getOrgScopeType() == null) {
            rule.setOrgScopeType(CodeOrgScopeType.GLOBAL);
        }
        normalizeScope(rule);
        if (rule.getLinkedUpdate() == null) {
            rule.setLinkedUpdate(Boolean.FALSE);
        }
        if (rule.getAllowRecycle() == null) {
            rule.setAllowRecycle(Boolean.FALSE);
        }
        validateTargetExists(rule);
        validatePrimaryUniqueness(rule);
        validateSequencePolicy(rule);
    }

    private void normalizeScope(CodeRule rule) {
        if (rule.getOrgScopeType() == CodeOrgScopeType.GLOBAL) {
            rule.setGlobalDefault(Boolean.TRUE);
            rule.setOrgScopeId(null);
            return;
        }
        if (rule.getOrgScopeId() == null || rule.getOrgScopeId().isBlank()) {
            throw new PlatformException("ORG scoped code rule requires orgScopeId");
        }
        rule.setGlobalDefault(Boolean.FALSE);
    }

    private void validateTargetExists(CodeRule rule) {
        moduleService.ifPresent(service -> {
            if (service.resolveVisibleModule(rule.getModuleAlias()) == null) {
                throw new PlatformException("Code rule requires existing module: " + rule.getModuleAlias());
            }
        });
        if (relationService.isEmpty() || fieldService.isEmpty()) {
            return;
        }
        ModuleMetadataRelation relation = relationService.get().list(Criteria.of()
                        .eq("moduleAlias", rule.getModuleAlias())
                        .eq("relationAlias", rule.getEntityAlias()),
                ALL).stream().findFirst().orElse(null);
        if (relation == null && metadataService.isPresent()) {
            Metadata metadata = metadataService.get().list(Criteria.of().eq("alias", rule.getEntityAlias()), ALL)
                    .stream()
                    .findFirst()
                    .orElse(null);
            if (metadata != null) {
                relation = relationService.get().list(Criteria.of()
                                .eq("moduleAlias", rule.getModuleAlias())
                                .eq("metadataId", metadata.getId()),
                        ALL).stream().findFirst().orElse(null);
            }
        }
        if (relation == null) {
            throw new PlatformException("Code rule requires existing module entity: "
                    + rule.getModuleAlias() + "/" + rule.getEntityAlias());
        }
        MetadataField field = resolveField(relation.getMetadataId(), rule);
        if (field == null) {
            throw new PlatformException("Code rule requires existing target field: "
                    + rule.getEntityAlias() + "." + rule.getFieldName());
        }
        if (rule.getMetadataFieldId() == null || rule.getMetadataFieldId().isBlank()) {
            rule.setMetadataFieldId(field.getId());
        }
        rule.setFieldName(field.getFieldName());
    }

    private MetadataField resolveField(String metadataId, CodeRule rule) {
        if (rule.getMetadataFieldId() != null && !rule.getMetadataFieldId().isBlank()) {
            MetadataField byId = fieldService.get().select(rule.getMetadataFieldId());
            if (byId != null && Objects.equals(byId.getMetadataId(), metadataId)) {
                return byId;
            }
        }
        return fieldService.get().list(Criteria.of()
                        .eq("metadataId", metadataId)
                        .eq("fieldName", rule.getFieldName()),
                ALL).stream().findFirst().orElse(null);
    }

    private void validatePrimaryUniqueness(CodeRule rule) {
        if (rule.getFieldRole() != CodeFieldRole.PRIMARY || Boolean.FALSE.equals(rule.getEnabled())) {
            return;
        }
        List<CodeRule> existingPrimaryRules = list(Criteria.of()
                .eq("moduleAlias", rule.getModuleAlias())
                .eq("entityAlias", rule.getEntityAlias())
                .eq("fieldRole", CodeFieldRole.PRIMARY)
                .eq("enabled", Boolean.TRUE), ALL);
        String currentFieldKey = targetFieldKey(rule);
        for (CodeRule existing : existingPrimaryRules) {
            if (Objects.equals(existing.getId(), rule.getId())) {
                continue;
            }
            if (!Objects.equals(targetFieldKey(existing), currentFieldKey)) {
                throw new PlatformException("Business object can only have one PRIMARY code field: "
                        + rule.getModuleAlias() + "/" + rule.getEntityAlias());
            }
            if (sameScope(existing, rule)) {
                throw new PlatformException("Code rule already exists for target field in the same scope: "
                        + rule.getModuleAlias() + "/" + rule.getEntityAlias() + "/" + currentFieldKey);
            }
        }
    }

    private boolean sameScope(CodeRule left, CodeRule right) {
        return Objects.equals(left.getOrgScopeType(), right.getOrgScopeType())
                && Objects.equals(left.getOrgScopeId(), right.getOrgScopeId());
    }

    private void validateSequencePolicy(CodeRule rule) {
        boolean hasSequence = rule.getSegments() != null && rule.getSegments().stream()
                .anyMatch(segment -> segment != null && segment.getSegmentType() == CodeSegmentType.SEQUENCE);
        if (hasSequence && rule.getSequencePolicy() == null) {
            throw new PlatformException("Code rule with SEQUENCE segment requires sequencePolicy");
        }
    }

    private void validateRuleTreeSemantics(CodeRule rule, List<CodeRuleSegment> segments, CodeSequencePolicy sequencePolicy) {
        CodeMode mode = rule.getMode() == null ? CodeMode.AUTO : rule.getMode();
        if (mode == CodeMode.MANUAL) {
            if (sequencePolicy != null || segments.stream().anyMatch(this::isSequenceSegment)) {
                throw new PlatformException("MANUAL code rule cannot declare automatic sequence configuration");
            }
            return;
        }
        if (segments.isEmpty()) {
            throw new PlatformException("Automatic code rule requires at least one segment");
        }
        boolean hasSequence = segments.stream().anyMatch(this::isSequenceSegment);
        if (hasSequence && sequencePolicy == null) {
            throw new PlatformException("Code rule with SEQUENCE segment requires sequencePolicy");
        }
        if (!hasSequence && sequencePolicy != null) {
            throw new PlatformException("Code sequence policy requires a SEQUENCE segment");
        }
        if (sequencePolicy != null) {
            validateDraftSequencePolicy(sequencePolicy);
        }
        if (Boolean.TRUE.equals(rule.getLinkedUpdate()) && !hasLinkedDependency(segments)) {
            throw new PlatformException("linkedUpdate code rule requires at least one source or formula segment");
        }
        for (CodeRuleSegment segment : segments) {
            validateDraftSegment(segment);
        }
    }

    private boolean isSequenceSegment(CodeRuleSegment segment) {
        return segment != null && segment.getSegmentType() == CodeSegmentType.SEQUENCE;
    }

    private boolean hasLinkedDependency(List<CodeRuleSegment> segments) {
        return segments.stream()
                .filter(Objects::nonNull)
                .anyMatch(segment -> segment.getSegmentType() == CodeSegmentType.FORMULA
                        || Set.of(CodeSegmentType.FIELD_VALUE, CodeSegmentType.CONTEXT_VAR, CodeSegmentType.VALUE_MAPPING)
                        .contains(segment.getSegmentType()) && hasText(segment.getSourceRef()));
    }

    private void validateDraftSequencePolicy(CodeSequencePolicy policy) {
        if (policy.getStartValue() != null && policy.getStartValue() < 0) {
            throw new PlatformException("Code sequence policy startValue must not be negative");
        }
        if (policy.getStepValue() != null && policy.getStepValue() <= 0) {
            throw new PlatformException("Code sequence policy stepValue must be positive");
        }
        if (policy.getSequenceLength() != null && policy.getSequenceLength() <= 0) {
            throw new PlatformException("Code sequence policy sequenceLength must be positive");
        }
        if (policy.getMaxValue() != null && policy.getStartValue() != null
                && policy.getMaxValue() < policy.getStartValue()) {
            throw new PlatformException("Code sequence policy maxValue must be greater than or equal to startValue");
        }
    }

    private void validateDraftSegment(CodeRuleSegment segment) {
        if (segment == null) {
            throw new PlatformException("Code rule segment must not be null");
        }
        if (segment.getSegmentType() == null) {
            throw new PlatformException("Code segment requires segmentType");
        }
        switch (segment.getSegmentType()) {
            case CONSTANT -> {
                if (!hasText(segment.getFixedValue())) {
                    throw new PlatformException("CONSTANT code segment requires fixedValue");
                }
            }
            case FIELD_VALUE, CONTEXT_VAR -> requireSourceRef(segment);
            case VALUE_MAPPING -> {
                requireSourceRef(segment);
                validateValueMappings(segment);
            }
            case FORMULA -> {
                if (!hasText(segment.getFormulaExpr())) {
                    throw new PlatformException("FORMULA code segment requires formulaExpr");
                }
            }
            case SEQUENCE -> {
                if (segment.getLength() != null && segment.getLength() <= 0) {
                    throw new PlatformException("SEQUENCE code segment length must be positive");
                }
            }
            case SYSTEM_TIME -> {
            }
        }
    }

    private void requireSourceRef(CodeRuleSegment segment) {
        if (!hasText(segment.getSourceRef())) {
            throw new PlatformException(segment.getSegmentType() + " code segment requires sourceRef");
        }
    }

    private void validateValueMappings(CodeRuleSegment segment) {
        List<CodeValueMapping> mappings = segment.getMappings() == null ? List.of() : segment.getMappings();
        if (mappings.isEmpty()) {
            throw new PlatformException("VALUE_MAPPING code segment requires mappings");
        }
        boolean defaultSeen = false;
        Set<String> sourceValues = new HashSet<>();
        for (CodeValueMapping mapping : mappings) {
            if (mapping == null) {
                throw new PlatformException("Code value mapping must not be null");
            }
            if (mapping.getTargetValue() == null) {
                throw new PlatformException("Code value mapping requires targetValue");
            }
            if (Boolean.TRUE.equals(mapping.getDefaultMapping())) {
                if (defaultSeen) {
                    throw new PlatformException("VALUE_MAPPING code segment can only have one default mapping");
                }
                defaultSeen = true;
            } else {
                if (!hasText(mapping.getSourceValue())) {
                    throw new PlatformException("Code value mapping requires sourceValue");
                }
                if (!sourceValues.add(mapping.getSourceValue())) {
                    throw new PlatformException("VALUE_MAPPING code segment has duplicate sourceValue: "
                            + mapping.getSourceValue());
                }
            }
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private ResolvedCodeRule resolveForField(List<CodeRule> candidates, String organizationId) {
        if (candidates == null || candidates.isEmpty()) {
            return null;
        }
        for (String currentOrganizationId : organizationChain(organizationId)) {
            List<CodeRule> scoped = candidates.stream()
                    .filter(rule -> rule.getOrgScopeType() == CodeOrgScopeType.ORG)
                    .filter(rule -> Objects.equals(rule.getOrgScopeId(), currentOrganizationId))
                    .toList();
            if (!scoped.isEmpty()) {
                return new ResolvedCodeRule(assertSingleScopedRule(scoped), currentOrganizationId);
            }
        }
        List<CodeRule> global = candidates.stream()
                .filter(rule -> rule.getOrgScopeType() == CodeOrgScopeType.GLOBAL)
                .filter(rule -> Boolean.TRUE.equals(rule.getGlobalDefault()))
                .filter(rule -> rule.getOrgScopeId() == null || rule.getOrgScopeId().isBlank())
                .toList();
        return global.isEmpty() ? null : new ResolvedCodeRule(assertSingleScopedRule(global), null);
    }

    private CodeRule assertSingleScopedRule(List<CodeRule> rules) {
        if (rules.size() > 1) {
            throw new PlatformException("Multiple code rules matched in the same scope");
        }
        return rules.getFirst();
    }

    private LinkedHashSet<String> organizationChain(String organizationId) {
        LinkedHashSet<String> chain = new LinkedHashSet<>();
        if (organizationId == null || organizationId.isBlank()) {
            return chain;
        }
        chain.add(organizationId);
        organizationHierarchyService.ifPresent(service -> chain.addAll(service.organizationIdsFromSelfToRoot(organizationId)));
        return chain;
    }

    private boolean isEffective(CodeRule rule, LocalDateTime at) {
        return (rule.getEffectiveFrom() == null || !rule.getEffectiveFrom().isAfter(at))
                && (rule.getEffectiveTo() == null || !rule.getEffectiveTo().isBefore(at));
    }

    private boolean matchesRequestedField(CodeRule rule, String metadataFieldId, String fieldName) {
        if (metadataFieldId != null && !metadataFieldId.isBlank()) {
            return Objects.equals(rule.getMetadataFieldId(), metadataFieldId);
        }
        if (fieldName != null && !fieldName.isBlank()) {
            return Objects.equals(rule.getFieldName(), fieldName);
        }
        return true;
    }

    private String targetFieldKey(CodeRule rule) {
        return rule.getMetadataFieldId() == null || rule.getMetadataFieldId().isBlank()
                ? rule.getFieldName()
                : rule.getMetadataFieldId();
    }

    private void validateResolveCommand(ResolveCodeRuleCommand command) {
        if (command == null) {
            throw new PlatformException("Code rule resolve command must not be null");
        }
        PlatformNameRules.requireModuleAlias(command.moduleAlias());
        PlatformNameRules.requireIdentifier(command.entityAlias(), "entityAlias");
    }
}
