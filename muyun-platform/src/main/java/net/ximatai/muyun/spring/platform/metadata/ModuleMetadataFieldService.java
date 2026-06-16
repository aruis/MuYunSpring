package net.ximatai.muyun.spring.platform.metadata;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.database.core.orm.Sort;
import net.ximatai.muyun.spring.ability.AbstractAbilityService;
import net.ximatai.muyun.spring.ability.BaseDao;
import net.ximatai.muyun.spring.ability.SoftDeleteAbility;
import net.ximatai.muyun.spring.ability.SortAbility;
import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.common.schema.PlatformAbilityFields;
import net.ximatai.muyun.spring.common.util.PlatformNameRules;
import net.ximatai.muyun.spring.dynamic.metadata.FieldMeasureUnitConversionMode;
import net.ximatai.muyun.spring.dynamic.metadata.FieldMeasureUnitMode;
import net.ximatai.muyun.spring.dynamic.metadata.FieldMoneyMode;
import net.ximatai.muyun.spring.dynamic.metadata.FieldType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@Service
public class ModuleMetadataFieldService extends AbstractAbilityService<ModuleMetadataField> implements
        SoftDeleteAbility<ModuleMetadataField>,
        SortAbility<ModuleMetadataField> {
    public static final String MODULE_ALIAS = "platform.module_metadata_field";
    private static final PageRequest ALL = new PageRequest(0, Integer.MAX_VALUE);

    private final ModuleMetadataRelationService relationService;
    private final MetadataService metadataService;
    private final MetadataFieldService fieldService;
    private final PlatformFieldTypeService fieldTypeService;
    private final ModuleMetadataFieldReferenceGenerateRuleValidator referenceGenerateRuleValidator;

    public ModuleMetadataFieldService(BaseDao<ModuleMetadataField, String> moduleMetadataFieldDao,
                                      ModuleMetadataRelationService relationService,
                                      MetadataService metadataService,
                                      MetadataFieldService fieldService) {
        this(moduleMetadataFieldDao, relationService, metadataService, fieldService, null, Optional.empty());
    }

    public ModuleMetadataFieldService(BaseDao<ModuleMetadataField, String> moduleMetadataFieldDao,
                                      ModuleMetadataRelationService relationService,
                                      MetadataService metadataService,
                                      MetadataFieldService fieldService,
                                      Optional<ModuleMetadataFieldReferenceGenerateRuleValidator> referenceGenerateRuleValidator) {
        this(moduleMetadataFieldDao, relationService, metadataService, fieldService, null, referenceGenerateRuleValidator);
    }

    @Autowired
    public ModuleMetadataFieldService(BaseDao<ModuleMetadataField, String> moduleMetadataFieldDao,
                                      ModuleMetadataRelationService relationService,
                                      MetadataService metadataService,
                                      MetadataFieldService fieldService,
                                      PlatformFieldTypeService fieldTypeService,
                                      Optional<ModuleMetadataFieldReferenceGenerateRuleValidator> referenceGenerateRuleValidator) {
        super(MODULE_ALIAS, ModuleMetadataField.class, moduleMetadataFieldDao);
        this.relationService = relationService;
        this.metadataService = metadataService;
        this.fieldService = fieldService;
        this.fieldTypeService = fieldTypeService;
        this.referenceGenerateRuleValidator = referenceGenerateRuleValidator == null
                ? null
                : referenceGenerateRuleValidator.orElse(null);
    }

    @Override
    public void beforeInsert(ModuleMetadataField moduleField) {
        normalizeAndValidate(moduleField);
    }

    @Override
    public void beforeUpdate(ModuleMetadataField moduleField) {
        normalizeAndValidate(moduleField);
    }

    @Override
    public Criteria sortScope(ModuleMetadataField moduleField) {
        return Criteria.of().eq("relationId", moduleField.getRelationId());
    }

    @Override
    public void validateSortScope(ModuleMetadataField left, ModuleMetadataField right) {
        if (!Objects.equals(left.getRelationId(), right.getRelationId())) {
            throw new PlatformException("Module metadata field sort can only move records within the same relation");
        }
    }

    public List<ModuleMetadataField> ensureForRelation(String relationId) {
        ModuleMetadataRelation relation = requireRelation(relationId);
        List<MetadataField> fields = fieldService.list(
                Criteria.of().eq("metadataId", relation.getMetadataId()),
                ALL,
                Sort.asc(PlatformAbilityFields.SORT_FIELD)
        );
        for (MetadataField field : fields) {
            if (findByRelationAndField(relation.getId(), field.getId()) == null) {
                ModuleMetadataField moduleField = new ModuleMetadataField();
                moduleField.setRelationId(relation.getId());
                moduleField.setMetadataFieldId(field.getId());
                moduleField.setTitle(field.getTitle());
                moduleField.setSortOrder(field.getSortOrder());
                insert(moduleField);
            }
        }
        return listByRelationId(relation.getId());
    }

    public List<ModuleMetadataField> listByRelationId(String relationId) {
        if (relationId == null || relationId.isBlank()) {
            return List.of();
        }
        return list(Criteria.of().eq("relationId", relationId), ALL, Sort.asc(PlatformAbilityFields.SORT_FIELD));
    }

    public List<ModuleMetadataField> listByModuleAlias(String moduleAlias) {
        String validAlias = PlatformNameRules.requireModuleAlias(moduleAlias);
        List<String> relationIds = relationService.list(Criteria.of().eq("moduleAlias", validAlias),
                        ALL, Sort.asc(PlatformAbilityFields.SORT_FIELD))
                .stream()
                .map(ModuleMetadataRelation::getId)
                .toList();
        if (relationIds.isEmpty()) {
            return List.of();
        }
        return list(Criteria.of().in("relationId", relationIds), ALL, Sort.asc(PlatformAbilityFields.SORT_FIELD));
    }

    public List<ModuleMetadataField> listMainByModuleAlias(String moduleAlias) {
        String validAlias = PlatformNameRules.requireModuleAlias(moduleAlias);
        List<String> relationIds = relationService.list(Criteria.of()
                                .eq("moduleAlias", validAlias)
                                .eq("relationRole", RelationRole.MAIN),
                        ALL, Sort.asc(PlatformAbilityFields.SORT_FIELD))
                .stream()
                .map(ModuleMetadataRelation::getId)
                .toList();
        if (relationIds.isEmpty()) {
            return List.of();
        }
        return list(Criteria.of().in("relationId", relationIds), ALL, Sort.asc(PlatformAbilityFields.SORT_FIELD));
    }

    public ResolvedModuleMetadataField resolve(String moduleMetadataFieldId) {
        ModuleMetadataField moduleField = moduleMetadataFieldId == null || moduleMetadataFieldId.isBlank()
                ? null
                : select(moduleMetadataFieldId);
        if (moduleField == null) {
            throw new PlatformException("Module metadata field requires existing config: " + moduleMetadataFieldId);
        }
        ModuleMetadataRelation relation = requireRelation(moduleField.getRelationId());
        Metadata metadata = requireMetadata(relation.getMetadataId());
        MetadataField field = requireField(moduleField.getMetadataFieldId());
        if (!metadata.getId().equals(field.getMetadataId())) {
            throw new PlatformException("Module metadata field metadata mismatch: " + moduleField.getId());
        }
        return new ResolvedModuleMetadataField(
                moduleField.getId(),
                relation.getModuleAlias(),
                relation.getId(),
                relation.getRelationAlias(),
                relation.getRelationRole(),
                metadata.getId(),
                metadata.getAlias(),
                metadata.getTitle(),
                field.getId(),
                field.getFieldName(),
                field.getColumnName(),
                field.getTitle(),
                field.getFieldTypeAlias()
        );
    }

    @Transactional
    public ModuleMetadataMeasureUnitPrepareResult prepareMeasureUnitConfig(
            String moduleMetadataFieldId,
            ModuleMetadataMeasureUnitPrepareCommand command) {
        ModuleMetadataField moduleField = moduleMetadataFieldId == null || moduleMetadataFieldId.isBlank()
                ? null
                : select(moduleMetadataFieldId);
        if (moduleField == null) {
            throw new PlatformException("Module metadata field requires existing config: " + moduleMetadataFieldId);
        }
        ModuleMetadataRelation relation = requireRelation(moduleField.getRelationId());
        Metadata metadata = requireMetadata(relation.getMetadataId());
        MetadataField owner = requireField(moduleField.getMetadataFieldId());
        if (!Objects.equals(owner.getMetadataId(), metadata.getId())) {
            throw new PlatformException("Module metadata field metadata mismatch: " + moduleMetadataFieldId);
        }
        if (owner.getFieldRole() == MetadataFieldRole.MEASURE_UNIT
                || owner.getFieldRole() == MetadataFieldRole.MEASURE_BASE_VALUE) {
            throw new PlatformException("measure unit config must be prepared on owner value field: "
                    + owner.getFieldName());
        }
        ModuleMetadataMeasureUnitPrepareCommand validCommand = command == null
                ? new ModuleMetadataMeasureUnitPrepareCommand(null, null, null, null, null, null,
                null, null, null, null, null, null, null)
                : command;
        requireNumericField(owner, "measure unit value field");
        String unitCategoryAlias = PlatformNameRules.requireIdentifier(
                validCommand.unitCategoryAlias(), "unitCategoryAlias");
        FieldMeasureUnitMode unitMode = validCommand.unitMode() == null
                ? FieldMeasureUnitMode.SELECTABLE
                : validCommand.unitMode();
        validatePrepareCommand(metadata, owner, validCommand, unitMode);
        MetadataField unitField = null;
        if (unitMode == FieldMeasureUnitMode.SELECTABLE) {
            unitField = ensureRelatedField(
                    metadata,
                    owner,
                    MetadataFieldForm.COMPANION,
                    MetadataFieldRole.MEASURE_UNIT,
                    defaultText(validCommand.unitFieldName(), owner.getFieldName() + "Unit"),
                    defaultText(validCommand.unitFieldTypeAlias(), "string"),
                    "Unit"
            );
        }
        MetadataField baseValueField = ensureRelatedField(
                metadata,
                owner,
                MetadataFieldForm.SHADOW,
                MetadataFieldRole.MEASURE_BASE_VALUE,
                defaultText(validCommand.baseValueFieldName(), owner.getFieldName() + "Base"),
                defaultText(validCommand.baseValueFieldTypeAlias(), owner.getFieldTypeAlias()),
                "Base"
        );
        moduleField.setUnitCategoryAlias(unitCategoryAlias);
        moduleField.setUnitMode(unitMode);
        moduleField.setFixedUnitCode(validCommand.fixedUnitCode());
        moduleField.setDefaultUnitCode(validCommand.defaultUnitCode());
        moduleField.setUnitFieldId(unitField == null ? null : unitField.getId());
        moduleField.setBaseValueFieldId(baseValueField.getId());
        moduleField.setBaseUnitCategoryAlias(validCommand.baseUnitCategoryAlias());
        moduleField.setBaseUnitCode(validCommand.baseUnitCode());
        moduleField.setUnitConversionMode(validCommand.unitConversionMode());
        moduleField.setConversionScopeFieldId(validCommand.conversionScopeFieldId());
        moduleField.setUnitRequired(validCommand.unitRequired());
        update(moduleField);
        return new ModuleMetadataMeasureUnitPrepareResult(select(moduleField.getId()), unitField, baseValueField);
    }

    @Transactional
    public ModuleMetadataMoneyPrepareResult prepareMoneyConfig(String moduleMetadataFieldId,
                                                              ModuleMetadataMoneyPrepareCommand command) {
        ModuleMetadataField moduleField = moduleMetadataFieldId == null || moduleMetadataFieldId.isBlank()
                ? null
                : select(moduleMetadataFieldId);
        if (moduleField == null) {
            throw new PlatformException("Module metadata field requires existing config: " + moduleMetadataFieldId);
        }
        ModuleMetadataRelation relation = requireRelation(moduleField.getRelationId());
        Metadata metadata = requireMetadata(relation.getMetadataId());
        MetadataField owner = requireField(moduleField.getMetadataFieldId());
        if (!Objects.equals(owner.getMetadataId(), metadata.getId())) {
            throw new PlatformException("Module metadata field metadata mismatch: " + moduleMetadataFieldId);
        }
        if (isMoneyRelatedRole(owner.getFieldRole())) {
            throw new PlatformException("money config must be prepared on owner amount field: "
                    + owner.getFieldName());
        }
        ModuleMetadataMoneyPrepareCommand validCommand = command == null
                ? new ModuleMetadataMoneyPrepareCommand(null, null, null, null, null, null,
                null, null, null, null, null, null, null, null)
                : command;
        requireNumericField(owner, "money amount field");
        FieldMoneyMode currencyMode = validCommand.currencyMode() == null
                ? FieldMoneyMode.SELECTABLE
                : validCommand.currencyMode();
        validateMoneyPrepareCommand(metadata, owner, validCommand, currencyMode);
        MetadataField currencyField = null;
        if (currencyMode == FieldMoneyMode.SELECTABLE) {
            currencyField = ensureRelatedField(
                    metadata,
                    owner,
                    MetadataFieldForm.COMPANION,
                    MetadataFieldRole.MONEY_CURRENCY,
                    defaultText(validCommand.currencyFieldName(), owner.getFieldName() + "Currency"),
                    defaultText(validCommand.currencyFieldTypeAlias(), "string"),
                    "Currency"
            );
        }
        MetadataField baseAmountField = ensureRelatedField(
                metadata,
                owner,
                MetadataFieldForm.SHADOW,
                MetadataFieldRole.MONEY_BASE_AMOUNT,
                defaultText(validCommand.baseAmountFieldName(), owner.getFieldName() + "Base"),
                defaultText(validCommand.baseAmountFieldTypeAlias(), owner.getFieldTypeAlias()),
                "Base Amount"
        );
        MetadataField exchangeRateField = null;
        if (Boolean.TRUE.equals(validCommand.createExchangeRateField())
                || hasText(validCommand.exchangeRateFieldName())) {
            exchangeRateField = ensureRelatedField(
                    metadata,
                    owner,
                    MetadataFieldForm.SHADOW,
                    MetadataFieldRole.MONEY_EXCHANGE_RATE,
                    defaultText(validCommand.exchangeRateFieldName(), owner.getFieldName() + "ExchangeRate"),
                    defaultText(validCommand.exchangeRateFieldTypeAlias(), "decimal"),
                    "Exchange Rate"
            );
        }
        moduleField.setMoneyCurrencyMode(currencyMode);
        moduleField.setMoneyFixedCurrencyCode(validCommand.fixedCurrencyCode());
        moduleField.setMoneyDefaultCurrencyCode(validCommand.defaultCurrencyCode());
        moduleField.setMoneyCurrencyFieldId(currencyField == null ? null : currencyField.getId());
        moduleField.setMoneyBaseAmountFieldId(baseAmountField.getId());
        moduleField.setMoneyBaseCurrencyCode(validCommand.baseCurrencyCode());
        moduleField.setMoneyRateTypeCode(validCommand.rateTypeCode());
        moduleField.setMoneyRateDateFieldId(validCommand.rateDateFieldId());
        moduleField.setMoneyExchangeRateFieldId(exchangeRateField == null ? null : exchangeRateField.getId());
        moduleField.setMoneyCurrencyRequired(validCommand.currencyRequired());
        update(moduleField);
        return new ModuleMetadataMoneyPrepareResult(select(moduleField.getId()), currencyField,
                baseAmountField, exchangeRateField);
    }

    private void normalizeAndValidate(ModuleMetadataField moduleField) {
        ModuleMetadataRelation relation = requireRelation(moduleField.getRelationId());
        Metadata metadata = requireMetadata(relation.getMetadataId());
        MetadataField field = requireField(moduleField.getMetadataFieldId());
        if (!relation.getMetadataId().equals(field.getMetadataId())) {
            throw new PlatformException("Module metadata field requires field in relation metadata: "
                    + moduleField.getMetadataFieldId());
        }
        normalizeReferenceConfig(moduleField, metadata, relation);
        normalizeMeasureUnitConfig(moduleField, metadata, field);
        normalizeMoneyConfig(moduleField, metadata, field);
        rejectDuplicate(moduleField, Criteria.of()
                        .eq("relationId", relation.getId())
                        .eq("metadataFieldId", field.getId()),
                "module metadata field must be unique: " + relation.getId() + "." + field.getId());
        moduleField.setRelationId(relation.getId());
        moduleField.setMetadataFieldId(field.getId());
    }

    private void normalizeReferenceConfig(ModuleMetadataField moduleField,
                                          Metadata metadata,
                                          ModuleMetadataRelation relation) {
        if (moduleField.getCloneable() == null) {
            moduleField.setCloneable(Boolean.FALSE);
        }
        boolean hasDictionaryApplication = hasText(moduleField.getDictionaryApplicationAlias());
        boolean hasDictionaryCategory = hasText(moduleField.getDictionaryCategoryAlias());
        if (!hasDictionaryApplication && !hasDictionaryCategory) {
            moduleField.setDictionaryApplicationAlias(null);
            moduleField.setDictionaryCategoryAlias(null);
        } else {
            if (!hasDictionaryCategory) {
                throw new PlatformException("dictionaryCategoryAlias must not be blank");
            }
            String applicationAlias = hasDictionaryApplication
                    ? PlatformNameRules.requireApplicationAlias(moduleField.getDictionaryApplicationAlias())
                    : metadata.getApplicationAlias();
            moduleField.setDictionaryApplicationAlias(applicationAlias);
            moduleField.setDictionaryCategoryAlias(PlatformNameRules.requireIdentifier(
                    moduleField.getDictionaryCategoryAlias(), "dictionaryCategoryAlias"));
        }
        boolean hasReferenceModule = hasText(moduleField.getReferenceModuleAlias());
        if (hasReferenceModule) {
            moduleField.setReferenceModuleAlias(PlatformNameRules.requireModuleAlias(moduleField.getReferenceModuleAlias()));
            moduleField.setReferenceModuleKeyField(PlatformNameRules.requireFieldName(
                    moduleField.getReferenceModuleKeyField(), "referenceModuleKeyField"));
            moduleField.setReferenceModuleLabelField(PlatformNameRules.requireFieldName(
                    moduleField.getReferenceModuleLabelField(), "referenceModuleLabelField"));
            if (hasText(moduleField.getReferenceGenerateRuleId()) && referenceGenerateRuleValidator == null) {
                throw new PlatformException("referenceGenerateRuleId requires generate rule validator");
            }
            if (hasText(moduleField.getReferenceGenerateRuleId())) {
                referenceGenerateRuleValidator.validateReferenceGenerateRule(
                        moduleField.getReferenceGenerateRuleId(),
                        moduleField.getReferenceModuleAlias(),
                        relation.getModuleAlias());
            }
        } else if (hasReferenceDependentConfig(moduleField)) {
            throw new PlatformException("reference module config requires referenceModuleAlias");
        }
        moduleField.setReferenceModulePlusFields(normalizeFieldNameSet(
                moduleField.getReferenceModulePlusFields(), "referenceModulePlusFields"));
    }

    private void normalizeMeasureUnitConfig(ModuleMetadataField moduleField,
                                            Metadata metadata,
                                            MetadataField field) {
        if (!hasText(moduleField.getUnitCategoryAlias())) {
            clearMeasureUnitConfig(moduleField);
            return;
        }
        if (field.getFieldRole() == MetadataFieldRole.MEASURE_UNIT
                || field.getFieldRole() == MetadataFieldRole.MEASURE_BASE_VALUE) {
            throw new PlatformException("measure unit config must be declared on owner value field: "
                    + field.getFieldName());
        }
        requireNumericField(field, "measure unit value field");
        moduleField.setUnitCategoryAlias(PlatformNameRules.requireIdentifier(
                moduleField.getUnitCategoryAlias(), "unitCategoryAlias"));
        if (hasText(moduleField.getBaseUnitCategoryAlias())) {
            moduleField.setBaseUnitCategoryAlias(PlatformNameRules.requireIdentifier(
                    moduleField.getBaseUnitCategoryAlias(), "baseUnitCategoryAlias"));
        } else {
            moduleField.setBaseUnitCategoryAlias(moduleField.getUnitCategoryAlias());
        }
        moduleField.setBaseUnitCode(PlatformNameRules.requireIdentifier(moduleField.getBaseUnitCode(), "baseUnitCode"));
        if (moduleField.getUnitConversionMode() == null) {
            moduleField.setUnitConversionMode(FieldMeasureUnitConversionMode.LINEAR);
        }
        if (moduleField.getUnitMode() == null) {
            moduleField.setUnitMode(hasText(moduleField.getFixedUnitCode())
                    ? FieldMeasureUnitMode.FIXED
                    : FieldMeasureUnitMode.SELECTABLE);
        }
        if (moduleField.getUnitRequired() == null) {
            moduleField.setUnitRequired(Boolean.FALSE);
        }
        if (hasText(moduleField.getDefaultUnitCode())) {
            moduleField.setDefaultUnitCode(PlatformNameRules.requireIdentifier(
                    moduleField.getDefaultUnitCode(), "defaultUnitCode"));
        }
        if (moduleField.getUnitMode() == FieldMeasureUnitMode.FIXED) {
            moduleField.setFixedUnitCode(PlatformNameRules.requireIdentifier(moduleField.getFixedUnitCode(), "fixedUnitCode"));
            moduleField.setUnitFieldId(null);
            if (!hasText(moduleField.getDefaultUnitCode())) {
                moduleField.setDefaultUnitCode(moduleField.getFixedUnitCode());
            }
        } else {
            moduleField.setFixedUnitCode(null);
            MetadataField unitField = requireRelatedField(moduleField.getUnitFieldId(), metadata, field,
                    MetadataFieldForm.COMPANION, MetadataFieldRole.MEASURE_UNIT, "unitFieldId");
            requireFieldType(unitField, FieldType.STRING, "measure unit companion field");
            moduleField.setUnitFieldId(unitField.getId());
        }
        MetadataField baseValueField = requireRelatedField(moduleField.getBaseValueFieldId(), metadata, field,
                MetadataFieldForm.SHADOW, MetadataFieldRole.MEASURE_BASE_VALUE, "baseValueFieldId");
        requireNumericField(baseValueField, "measure base value shadow field");
        moduleField.setBaseValueFieldId(baseValueField.getId());
        if (hasText(moduleField.getConversionScopeFieldId())) {
            MetadataField scopeField = requireField(moduleField.getConversionScopeFieldId());
            if (!Objects.equals(scopeField.getMetadataId(), metadata.getId())) {
                throw new PlatformException("conversionScopeFieldId must belong to same metadata: "
                        + moduleField.getConversionScopeFieldId());
            }
            moduleField.setConversionScopeFieldId(scopeField.getId());
        }
    }

    private void clearMeasureUnitConfig(ModuleMetadataField moduleField) {
        moduleField.setUnitCategoryAlias(null);
        moduleField.setUnitMode(null);
        moduleField.setFixedUnitCode(null);
        moduleField.setDefaultUnitCode(null);
        moduleField.setUnitFieldId(null);
        moduleField.setBaseValueFieldId(null);
        moduleField.setBaseUnitCategoryAlias(null);
        moduleField.setBaseUnitCode(null);
        moduleField.setUnitConversionMode(null);
        moduleField.setConversionScopeFieldId(null);
        moduleField.setUnitRequired(Boolean.FALSE);
    }

    private void normalizeMoneyConfig(ModuleMetadataField moduleField,
                                      Metadata metadata,
                                      MetadataField field) {
        if (moduleField.getMoneyCurrencyMode() == null
                && !hasText(moduleField.getMoneyBaseAmountFieldId())
                && !hasText(moduleField.getMoneyRateTypeCode())) {
            clearMoneyConfig(moduleField);
            return;
        }
        if (isMoneyRelatedRole(field.getFieldRole())) {
            throw new PlatformException("money config must be declared on owner amount field: "
                    + field.getFieldName());
        }
        requireNumericField(field, "money amount field");
        if (moduleField.getMoneyCurrencyMode() == null) {
            moduleField.setMoneyCurrencyMode(hasText(moduleField.getMoneyFixedCurrencyCode())
                    ? FieldMoneyMode.FIXED
                    : FieldMoneyMode.SELECTABLE);
        }
        if (hasText(moduleField.getMoneyDefaultCurrencyCode())) {
            moduleField.setMoneyDefaultCurrencyCode(requireCurrencyCode(
                    moduleField.getMoneyDefaultCurrencyCode(), "moneyDefaultCurrencyCode"));
        }
        if (hasText(moduleField.getMoneyBaseCurrencyCode())) {
            moduleField.setMoneyBaseCurrencyCode(requireCurrencyCode(
                    moduleField.getMoneyBaseCurrencyCode(), "moneyBaseCurrencyCode"));
        }
        moduleField.setMoneyRateTypeCode(requireRateTypeCode(moduleField.getMoneyRateTypeCode(), "moneyRateTypeCode"));
        if (moduleField.getMoneyCurrencyRequired() == null) {
            moduleField.setMoneyCurrencyRequired(Boolean.TRUE);
        }
        if (moduleField.getMoneyCurrencyMode() == FieldMoneyMode.FIXED) {
            moduleField.setMoneyFixedCurrencyCode(requireCurrencyCode(
                    moduleField.getMoneyFixedCurrencyCode(), "moneyFixedCurrencyCode"));
            moduleField.setMoneyCurrencyFieldId(null);
            if (!hasText(moduleField.getMoneyDefaultCurrencyCode())) {
                moduleField.setMoneyDefaultCurrencyCode(moduleField.getMoneyFixedCurrencyCode());
            }
        } else {
            moduleField.setMoneyFixedCurrencyCode(null);
            MetadataField currencyField = requireRelatedField(moduleField.getMoneyCurrencyFieldId(), metadata, field,
                    MetadataFieldForm.COMPANION, MetadataFieldRole.MONEY_CURRENCY, "moneyCurrencyFieldId");
            requireTextField(currencyField, "money currency companion field");
            moduleField.setMoneyCurrencyFieldId(currencyField.getId());
        }
        MetadataField baseAmountField = requireRelatedField(moduleField.getMoneyBaseAmountFieldId(), metadata, field,
                MetadataFieldForm.SHADOW, MetadataFieldRole.MONEY_BASE_AMOUNT, "moneyBaseAmountFieldId");
        requireNumericField(baseAmountField, "money base amount shadow field");
        moduleField.setMoneyBaseAmountFieldId(baseAmountField.getId());
        if (hasText(moduleField.getMoneyRateDateFieldId())) {
            MetadataField rateDateField = requireField(moduleField.getMoneyRateDateFieldId());
            if (!Objects.equals(rateDateField.getMetadataId(), metadata.getId())) {
                throw new PlatformException("moneyRateDateFieldId must belong to same metadata: "
                        + moduleField.getMoneyRateDateFieldId());
            }
            FieldType type = requireFieldType(rateDateField);
            if (type != FieldType.DATE && type != FieldType.TIMESTAMP && type != FieldType.ZONED_TIMESTAMP) {
                throw new PlatformException("money rate date field requires date or timestamp field: "
                        + rateDateField.getFieldName());
            }
            moduleField.setMoneyRateDateFieldId(rateDateField.getId());
        }
        if (hasText(moduleField.getMoneyExchangeRateFieldId())) {
            MetadataField exchangeRateField = requireRelatedField(moduleField.getMoneyExchangeRateFieldId(), metadata, field,
                    MetadataFieldForm.SHADOW, MetadataFieldRole.MONEY_EXCHANGE_RATE, "moneyExchangeRateFieldId");
            requireNumericField(exchangeRateField, "money exchange rate shadow field");
            moduleField.setMoneyExchangeRateFieldId(exchangeRateField.getId());
        }
    }

    private void clearMoneyConfig(ModuleMetadataField moduleField) {
        moduleField.setMoneyCurrencyMode(null);
        moduleField.setMoneyFixedCurrencyCode(null);
        moduleField.setMoneyDefaultCurrencyCode(null);
        moduleField.setMoneyCurrencyFieldId(null);
        moduleField.setMoneyBaseAmountFieldId(null);
        moduleField.setMoneyBaseCurrencyCode(null);
        moduleField.setMoneyRateTypeCode(null);
        moduleField.setMoneyRateDateFieldId(null);
        moduleField.setMoneyExchangeRateFieldId(null);
        moduleField.setMoneyCurrencyRequired(Boolean.TRUE);
    }

    private MetadataField requireRelatedField(String fieldId,
                                              Metadata metadata,
                                              MetadataField owner,
                                              MetadataFieldForm form,
                                              MetadataFieldRole role,
                                              String label) {
        if (!hasText(fieldId)) {
            throw new PlatformException(label + " must not be blank");
        }
        MetadataField related = requireField(fieldId);
        if (!Objects.equals(related.getMetadataId(), metadata.getId())) {
            throw new PlatformException(label + " must belong to same metadata: " + fieldId);
        }
        if (related.getFieldForm() != form || related.getFieldRole() != role) {
            throw new PlatformException(label + " requires " + form + " " + role + " field: " + fieldId);
        }
        if (!Objects.equals(related.getOwnerFieldId(), owner.getId())) {
            throw new PlatformException(label + " must be owned by owner value field: " + fieldId);
        }
        return related;
    }

    private MetadataField ensureRelatedField(Metadata metadata,
                                             MetadataField owner,
                                             MetadataFieldForm form,
                                             MetadataFieldRole role,
                                             String fieldName,
                                             String fieldTypeAlias,
                                             String titleSuffix) {
        MetadataField existing = findOneRelatedField(metadata.getId(), owner.getId(), role);
        if (existing != null) {
            if (existing.getFieldForm() != form) {
                throw new PlatformException("related field form mismatch: " + existing.getFieldName());
            }
            return existing;
        }
        String validFieldName = PlatformNameRules.requireFieldName(fieldName, "relatedFieldName");
        rejectRelatedFieldNameCollision(metadata.getId(), validFieldName, role);
        rejectRelatedColumnNameCollision(metadata.getId(), toColumnName(validFieldName));
        MetadataField field = new MetadataField();
        field.setMetadataId(metadata.getId());
        field.setFieldName(validFieldName);
        field.setColumnName(toColumnName(validFieldName));
        field.setFieldTypeAlias(PlatformNameRules.requireIdentifier(fieldTypeAlias, "relatedFieldTypeAlias"));
        field.setTitle(defaultText(owner.getTitle(), owner.getFieldName()) + " " + titleSuffix);
        field.setFieldForm(form);
        field.setFieldRole(role);
        field.setOwnerFieldId(owner.getId());
        field.setSystemManaged(role == MetadataFieldRole.MEASURE_BASE_VALUE
                || role == MetadataFieldRole.MONEY_BASE_AMOUNT
                || role == MetadataFieldRole.MONEY_EXCHANGE_RATE);
        field.setSortOrder(nextSortOrder(metadata.getId()));
        String id = fieldService.insert(field);
        return fieldService.select(id);
    }

    private void validatePrepareCommand(Metadata metadata,
                                        MetadataField owner,
                                        ModuleMetadataMeasureUnitPrepareCommand command,
                                        FieldMeasureUnitMode unitMode) {
        PlatformNameRules.requireIdentifier(command.unitCategoryAlias(), "unitCategoryAlias");
        PlatformNameRules.requireIdentifier(command.baseUnitCode(), "baseUnitCode");
        if (hasText(command.baseUnitCategoryAlias())) {
            PlatformNameRules.requireIdentifier(command.baseUnitCategoryAlias(), "baseUnitCategoryAlias");
        }
        if (hasText(command.defaultUnitCode())) {
            PlatformNameRules.requireIdentifier(command.defaultUnitCode(), "defaultUnitCode");
        }
        if (unitMode == FieldMeasureUnitMode.FIXED) {
            PlatformNameRules.requireIdentifier(command.fixedUnitCode(), "fixedUnitCode");
        } else {
            validatePreparedFieldSpec(metadata, owner, MetadataFieldRole.MEASURE_UNIT,
                    defaultText(command.unitFieldName(), owner.getFieldName() + "Unit"),
                    defaultText(command.unitFieldTypeAlias(), "string"),
                    FieldType.STRING);
        }
        validatePreparedFieldSpec(metadata, owner, MetadataFieldRole.MEASURE_BASE_VALUE,
                defaultText(command.baseValueFieldName(), owner.getFieldName() + "Base"),
                defaultText(command.baseValueFieldTypeAlias(), owner.getFieldTypeAlias()),
                null);
        if (hasText(command.conversionScopeFieldId())) {
            MetadataField scopeField = requireField(command.conversionScopeFieldId());
            if (!Objects.equals(scopeField.getMetadataId(), metadata.getId())) {
                throw new PlatformException("conversionScopeFieldId must belong to same metadata: "
                        + command.conversionScopeFieldId());
            }
        }
    }

    private void validateMoneyPrepareCommand(Metadata metadata,
                                             MetadataField owner,
                                             ModuleMetadataMoneyPrepareCommand command,
                                             FieldMoneyMode currencyMode) {
        if (currencyMode == FieldMoneyMode.FIXED) {
            requireCurrencyCode(command.fixedCurrencyCode(), "fixedCurrencyCode");
        } else {
            validatePreparedFieldSpec(metadata, owner, MetadataFieldRole.MONEY_CURRENCY,
                    defaultText(command.currencyFieldName(), owner.getFieldName() + "Currency"),
                    defaultText(command.currencyFieldTypeAlias(), "string"),
                    FieldType.STRING);
        }
        if (hasText(command.defaultCurrencyCode())) {
            requireCurrencyCode(command.defaultCurrencyCode(), "defaultCurrencyCode");
        }
        if (hasText(command.baseCurrencyCode())) {
            requireCurrencyCode(command.baseCurrencyCode(), "baseCurrencyCode");
        }
        requireRateTypeCode(command.rateTypeCode(), "rateTypeCode");
        validatePreparedFieldSpec(metadata, owner, MetadataFieldRole.MONEY_BASE_AMOUNT,
                defaultText(command.baseAmountFieldName(), owner.getFieldName() + "Base"),
                defaultText(command.baseAmountFieldTypeAlias(), owner.getFieldTypeAlias()),
                null);
        if (Boolean.TRUE.equals(command.createExchangeRateField())
                || hasText(command.exchangeRateFieldName())) {
            validatePreparedFieldSpec(metadata, owner, MetadataFieldRole.MONEY_EXCHANGE_RATE,
                    defaultText(command.exchangeRateFieldName(), owner.getFieldName() + "ExchangeRate"),
                    defaultText(command.exchangeRateFieldTypeAlias(), "decimal"),
                    null);
        }
        if (hasText(command.rateDateFieldId())) {
            MetadataField rateDateField = requireField(command.rateDateFieldId());
            if (!Objects.equals(rateDateField.getMetadataId(), metadata.getId())) {
                throw new PlatformException("rateDateFieldId must belong to same metadata: "
                        + command.rateDateFieldId());
            }
            FieldType type = requireFieldType(rateDateField);
            if (type != FieldType.DATE && type != FieldType.TIMESTAMP && type != FieldType.ZONED_TIMESTAMP) {
                throw new PlatformException("money rate date field requires date or timestamp field: "
                        + rateDateField.getFieldName());
            }
        }
    }

    private void validatePreparedFieldSpec(Metadata metadata,
                                           MetadataField owner,
                                           MetadataFieldRole role,
                                           String fieldName,
                                           String fieldTypeAlias,
                                           FieldType expectedType) {
        MetadataField existing = findOneRelatedField(metadata.getId(), owner.getId(), role);
        if (existing != null) {
            return;
        }
        String validFieldName = PlatformNameRules.requireFieldName(fieldName, "relatedFieldName");
        rejectRelatedFieldNameCollision(metadata.getId(), validFieldName, role);
        rejectRelatedColumnNameCollision(metadata.getId(), toColumnName(validFieldName));
        PlatformFieldType fieldType = fieldTypeService.requireFieldType(
                PlatformNameRules.requireIdentifier(fieldTypeAlias, "relatedFieldTypeAlias"));
        if (expectedType != null && fieldType.getFieldType() != expectedType) {
            throw new PlatformException("related field requires " + expectedType + " field type: "
                    + validFieldName);
        }
        if ((role == MetadataFieldRole.MEASURE_BASE_VALUE
                || role == MetadataFieldRole.MONEY_BASE_AMOUNT
                || role == MetadataFieldRole.MONEY_EXCHANGE_RATE)
                && fieldType.getFieldType() != FieldType.INTEGER
                && fieldType.getFieldType() != FieldType.LONG
                && fieldType.getFieldType() != FieldType.DECIMAL) {
            throw new PlatformException("related numeric field requires numeric field type: " + validFieldName);
        }
    }

    private MetadataField findOneRelatedField(String metadataId, String ownerFieldId, MetadataFieldRole role) {
        List<MetadataField> fields = fieldService.list(Criteria.of()
                        .eq("metadataId", metadataId)
                        .eq("ownerFieldId", ownerFieldId)
                        .eq("fieldRole", role),
                ALL, Sort.asc(PlatformAbilityFields.SORT_FIELD));
        if (fields.size() > 1) {
            throw new PlatformException("related field must be unique for owner and role: "
                    + ownerFieldId + "." + role);
        }
        return fields.stream().findFirst().orElse(null);
    }

    private void rejectRelatedFieldNameCollision(String metadataId, String fieldName, MetadataFieldRole role) {
        MetadataField existing = fieldService.list(Criteria.of()
                        .eq("metadataId", metadataId)
                        .eq("fieldName", fieldName),
                PageRequest.of(1, 1)).stream().findFirst().orElse(null);
        if (existing != null) {
            throw new PlatformException("related field name is already used: " + fieldName);
        }
    }

    private void rejectRelatedColumnNameCollision(String metadataId, String columnName) {
        MetadataField existing = fieldService.list(Criteria.of()
                        .eq("metadataId", metadataId)
                        .eq("columnName", columnName),
                PageRequest.of(1, 1)).stream().findFirst().orElse(null);
        if (existing != null) {
            throw new PlatformException("related column name is already used: " + columnName);
        }
    }

    private Integer nextSortOrder(String metadataId) {
        return fieldService.list(Criteria.of().eq("metadataId", metadataId), ALL, Sort.asc(PlatformAbilityFields.SORT_FIELD))
                .stream()
                .map(MetadataField::getSortOrder)
                .filter(Objects::nonNull)
                .max(Integer::compareTo)
                .map(value -> value + 10)
                .orElse(10);
    }

    private String toColumnName(String fieldName) {
        StringBuilder column = new StringBuilder();
        for (int i = 0; i < fieldName.length(); i++) {
            char ch = fieldName.charAt(i);
            if (Character.isUpperCase(ch)) {
                if (i > 0) {
                    column.append('_');
                }
                column.append(Character.toLowerCase(ch));
            } else {
                column.append(ch);
            }
        }
        return PlatformNameRules.requireDatabaseName(column.toString(), "relatedColumnName");
    }

    private String defaultText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private void requireNumericField(MetadataField field, String label) {
        FieldType type = requireFieldType(field);
        if (type != FieldType.INTEGER && type != FieldType.LONG && type != FieldType.DECIMAL) {
            throw new PlatformException(label + " requires numeric field: " + field.getFieldName());
        }
    }

    private void requireFieldType(MetadataField field, FieldType expected, String label) {
        FieldType type = requireFieldType(field);
        if (type != expected) {
            throw new PlatformException(label + " requires " + expected + " field: " + field.getFieldName());
        }
    }

    private void requireTextField(MetadataField field, String label) {
        FieldType type = requireFieldType(field);
        if (type != FieldType.STRING && type != FieldType.TEXT) {
            throw new PlatformException(label + " requires text field: " + field.getFieldName());
        }
    }

    private FieldType requireFieldType(MetadataField field) {
        if (fieldTypeService == null) {
            throw new PlatformException("module field config requires PlatformFieldTypeService");
        }
        return fieldTypeService.requireFieldType(field.getFieldTypeAlias()).getFieldType();
    }

    private boolean isMoneyRelatedRole(MetadataFieldRole role) {
        return role == MetadataFieldRole.MONEY_CURRENCY
                || role == MetadataFieldRole.MONEY_BASE_AMOUNT
                || role == MetadataFieldRole.MONEY_EXCHANGE_RATE;
    }

    private String requireCurrencyCode(String value, String label) {
        if (!hasText(value)) {
            throw new PlatformException(label + " must not be blank");
        }
        String code = value.trim().toUpperCase();
        if (!code.matches("[A-Z]{3}")) {
            throw new PlatformException(label + " must be ISO 4217 alpha-3 code: " + value);
        }
        return code;
    }

    private String requireRateTypeCode(String value, String label) {
        if (!hasText(value)) {
            throw new PlatformException(label + " must not be blank");
        }
        String code = value.trim().toUpperCase();
        if (!code.matches("[A-Z][A-Z0-9_]{0,63}")) {
            throw new PlatformException(label + " must use upper snake code: " + value);
        }
        return code;
    }

    private boolean hasReferenceDependentConfig(ModuleMetadataField moduleField) {
        return hasText(moduleField.getReferenceModuleKeyField())
                || hasText(moduleField.getReferenceModuleLabelField())
                || hasText(moduleField.getReferenceGenerateRuleId())
                || hasText(moduleField.getReferenceQueryTemplateId())
                || (moduleField.getReferenceModulePlusFields() != null
                && !moduleField.getReferenceModulePlusFields().isEmpty());
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private Set<String> normalizeFieldNameSet(Set<String> fields, String label) {
        if (fields == null || fields.isEmpty()) {
            return fields;
        }
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String field : fields) {
            normalized.add(PlatformNameRules.requireFieldName(field, label));
        }
        return normalized;
    }

    private ModuleMetadataField findByRelationAndField(String relationId, String fieldId) {
        return findOne(Criteria.of()
                .eq("relationId", relationId)
                .eq("metadataFieldId", fieldId));
    }

    private ModuleMetadataRelation requireRelation(String relationId) {
        ModuleMetadataRelation relation = relationId == null || relationId.isBlank() ? null : relationService.select(relationId);
        if (relation == null) {
            throw new PlatformException("Module metadata field requires existing relation: " + relationId);
        }
        return relation;
    }

    private Metadata requireMetadata(String metadataId) {
        Metadata metadata = metadataId == null || metadataId.isBlank() ? null : metadataService.select(metadataId);
        if (metadata == null) {
            throw new PlatformException("Module metadata field requires existing metadata: " + metadataId);
        }
        return metadata;
    }

    private MetadataField requireField(String fieldId) {
        MetadataField field = fieldId == null || fieldId.isBlank() ? null : fieldService.select(fieldId);
        if (field == null) {
            throw new PlatformException("Module metadata field requires existing field: " + fieldId);
        }
        return field;
    }
}
