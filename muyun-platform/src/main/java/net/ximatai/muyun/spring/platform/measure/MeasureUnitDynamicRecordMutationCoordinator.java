package net.ximatai.muyun.spring.platform.measure;

import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.common.util.PlatformNameRules;
import net.ximatai.muyun.spring.dynamic.metadata.FieldDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.FieldMeasureUnitConversionMode;
import net.ximatai.muyun.spring.dynamic.metadata.FieldMeasureUnitDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.FieldMeasureUnitMode;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicRecord;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicRecordMutationCoordinator;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;

@Component
@Order(Ordered.LOWEST_PRECEDENCE - 100)
public class MeasureUnitDynamicRecordMutationCoordinator implements DynamicRecordMutationCoordinator {
    private final MeasureUnitConversionService linearConversionService;
    private final MeasureUnitBusinessConversionService businessConversionService;
    private final Clock clock;

    public MeasureUnitDynamicRecordMutationCoordinator(MeasureUnitConversionService linearConversionService,
                                                       MeasureUnitBusinessConversionService businessConversionService) {
        this(linearConversionService, businessConversionService, Clock.systemDefaultZone());
    }

    public MeasureUnitDynamicRecordMutationCoordinator(MeasureUnitConversionService linearConversionService,
                                                       MeasureUnitBusinessConversionService businessConversionService,
                                                       Clock clock) {
        this.linearConversionService = linearConversionService;
        this.businessConversionService = businessConversionService;
        this.clock = clock == null ? Clock.systemDefaultZone() : clock;
    }

    @Override
    public void beforeCreate(String moduleAlias, String entityAlias, DynamicRecord record) {
        normalizeMeasureFields(moduleAlias, record, null, true);
    }

    @Override
    public void beforeRelationChildCreate(String moduleAlias,
                                          String parentEntityAlias,
                                          String relationCode,
                                          String childEntityAlias,
                                          DynamicRecord parent,
                                          DynamicRecord child) {
        normalizeMeasureFields(moduleAlias, child, null, true);
    }

    @Override
    public void beforeUpdate(String moduleAlias, String entityAlias, DynamicRecord before, DynamicRecord incoming) {
        normalizeMeasureFields(moduleAlias, incoming, before, false);
    }

    @Override
    public void beforeRelationChildUpdate(String moduleAlias,
                                          String parentEntityAlias,
                                          String relationCode,
                                          String childEntityAlias,
                                          DynamicRecord parentBefore,
                                          DynamicRecord parentIncoming,
                                          DynamicRecord childBefore,
                                          DynamicRecord childIncoming) {
        normalizeMeasureFields(moduleAlias, childIncoming, childBefore, false);
    }

    private void normalizeMeasureFields(String moduleAlias,
                                        DynamicRecord incoming,
                                        DynamicRecord before,
                                        boolean create) {
        if (incoming == null) {
            return;
        }
        String applicationAlias = applicationAlias(moduleAlias);
        for (FieldDefinition field : incoming.getEntity().fields()) {
            FieldMeasureUnitDefinition measure = field.measureUnit();
            if (measure == null || !measure.enabled()) {
                continue;
            }
            normalizeMeasureField(applicationAlias, moduleAlias, incoming, before, create, field, measure);
        }
    }

    private void normalizeMeasureField(String applicationAlias,
                                       String moduleAlias,
                                       DynamicRecord incoming,
                                       DynamicRecord before,
                                       boolean create,
                                       FieldDefinition field,
                                       FieldMeasureUnitDefinition measure) {
        boolean valueChanged = create || incoming.isExplicitlySet(field.fieldName());
        boolean unitChanged = create || measure.unitFieldName() != null
                && incoming.isExplicitlySet(measure.unitFieldName());
        if (!valueChanged && !unitChanged) {
            return;
        }
        Object value = valueChanged
                ? incoming.getPlatformValues().get(field.fieldName())
                : valueOf(before, field.fieldName());
        if (value == null) {
            incoming.putGeneratedValue(measure.baseValueFieldName(), null);
            return;
        }
        String unitCode = resolveUnitCode(incoming, before, create, measure);
        BigDecimal baseValue = convert(applicationAlias, moduleAlias, incoming, before, value, unitCode, measure);
        incoming.putGeneratedValue(measure.baseValueFieldName(), baseValue);
    }

    private String resolveUnitCode(DynamicRecord incoming,
                                   DynamicRecord before,
                                   boolean create,
                                   FieldMeasureUnitDefinition measure) {
        if (measure.mode() == FieldMeasureUnitMode.FIXED) {
            return PlatformNameRules.requireCode(measure.fixedUnitCode(), "fixedUnitCode");
        }
        boolean unitExplicitlySet = incoming.isExplicitlySet(measure.unitFieldName());
        String unitCode = stringValue(incoming.getPlatformValues().get(measure.unitFieldName()));
        if (unitExplicitlySet && unitCode == null) {
            throw new PlatformException("measure unit field must not be blank when measure value exists: "
                    + measure.unitFieldName());
        }
        if (unitCode == null && before != null) {
            unitCode = stringValue(valueOf(before, measure.unitFieldName()));
        }
        if (unitCode == null && measure.defaultUnitCode() != null && !measure.defaultUnitCode().isBlank()) {
            unitCode = PlatformNameRules.requireCode(measure.defaultUnitCode(), "defaultUnitCode");
            incoming.putGeneratedValue(measure.unitFieldName(), unitCode);
        }
        if (unitCode == null) {
            throw new PlatformException("measure unit field is required when measure value exists: "
                    + measure.unitFieldName());
        }
        return PlatformNameRules.requireCode(unitCode, "measureUnitCode");
    }

    private BigDecimal convert(String applicationAlias,
                               String moduleAlias,
                               DynamicRecord incoming,
                               DynamicRecord before,
                               Object value,
                               String unitCode,
                               FieldMeasureUnitDefinition measure) {
        BigDecimal numericValue = bigDecimal(value);
        String baseCategoryAlias = measure.baseUnitCategoryAlias() == null || measure.baseUnitCategoryAlias().isBlank()
                ? measure.categoryAlias()
                : measure.baseUnitCategoryAlias();
        if (measure.conversionMode() == FieldMeasureUnitConversionMode.BUSINESS_RULE) {
            MeasureUnitBusinessConversion conversion = businessConversionService.convert(
                    context(applicationAlias, moduleAlias, incoming, before, measure),
                    numericValue,
                    measure.categoryAlias(),
                    unitCode,
                    baseCategoryAlias,
                    measure.baseUnitCode()
            );
            return conversion.convertedValue();
        }
        if (!measure.categoryAlias().equals(baseCategoryAlias)) {
            throw new PlatformException("linear measure conversion requires same base unit category: "
                    + measure.categoryAlias() + " -> " + baseCategoryAlias);
        }
        return linearConversionService.convert(applicationAlias, measure.categoryAlias(), numericValue,
                unitCode, measure.baseUnitCode()).convertedValue();
    }

    private MeasureUnitConversionContext context(String applicationAlias,
                                                 String moduleAlias,
                                                 DynamicRecord incoming,
                                                 DynamicRecord before,
                                                 FieldMeasureUnitDefinition measure) {
        String contextObjectType = null;
        String contextObjectId = null;
        if (measure.conversionScopeFieldName() != null && !measure.conversionScopeFieldName().isBlank()) {
            contextObjectType = contextObjectType(measure.conversionScopeFieldName());
            Object scopeValue = incoming.getPlatformValues().get(measure.conversionScopeFieldName());
            if (scopeValue == null) {
                scopeValue = valueOf(before, measure.conversionScopeFieldName());
            }
            contextObjectId = scopeValue == null ? null : String.valueOf(scopeValue);
        }
        return new MeasureUnitConversionContext(applicationAlias, moduleAlias, contextObjectType, contextObjectId,
                LocalDateTime.ofInstant(clock.instant(), ZoneId.systemDefault()));
    }

    private String contextObjectType(String fieldName) {
        StringBuilder normalized = new StringBuilder();
        for (int i = 0; i < fieldName.length(); i++) {
            char ch = fieldName.charAt(i);
            if (Character.isUpperCase(ch)) {
                if (i > 0) {
                    normalized.append('_');
                }
                normalized.append(Character.toLowerCase(ch));
            } else {
                normalized.append(ch);
            }
        }
        return normalized.toString();
    }

    private Object valueOf(DynamicRecord record, String fieldName) {
        return record == null ? null : record.getPlatformValues().get(fieldName);
    }

    private String stringValue(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value);
        return text.isBlank() ? null : text;
    }

    private BigDecimal bigDecimal(Object value) {
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        if (value instanceof Number number) {
            return new BigDecimal(number.toString());
        }
        return new BigDecimal(String.valueOf(value));
    }

    private String applicationAlias(String moduleAlias) {
        String validModuleAlias = PlatformNameRules.requireModuleAlias(moduleAlias);
        int dot = validModuleAlias.indexOf('.');
        if (dot < 1) {
            throw new PlatformException("moduleAlias must include application alias: " + moduleAlias);
        }
        return validModuleAlias.substring(0, dot);
    }
}
