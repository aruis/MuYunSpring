package net.ximatai.muyun.spring.platform.config;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class LowCodeMoneyHealthChecker implements LowCodeModuleHealthChecker {
    private static final Set<String> NUMERIC_TYPES = Set.of("DECIMAL", "NUMERIC", "NUMBER", "INTEGER", "INT",
            "LONG", "BIGINT");
    private static final Set<String> TEXT_TYPES = Set.of("STRING", "TEXT", "VARCHAR", "CHAR");
    private static final Set<String> DATE_TYPES = Set.of("DATE", "TIMESTAMP", "ZONED_TIMESTAMP", "DATETIME");

    @Override
    public List<LowCodeConfigHealthItem> check(LowCodeModuleHealthContext context) {
        LowCodeModulePackage modulePackage = context == null ? null : context.modulePackage();
        if (modulePackage == null || !modulePackage.includes(LowCodePackageBundleType.METADATA)) {
            return List.of();
        }
        LowCodeConfigBundle bundle = modulePackage.bundleMap().get(LowCodePackageBundleType.METADATA);
        if (bundle == null || bundle.content().isEmpty()) {
            return List.of();
        }
        List<Map<String, Object>> rawFields = fields(bundle.content());
        List<FieldContract> fields = contracts(rawFields);
        if (fields.isEmpty()) {
            return List.of();
        }
        Set<String> fieldNames = fields.stream()
                .map(FieldContract::fieldName)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<String, String> fieldTypes = fields.stream()
                .filter(field -> field.fieldName() != null)
                .filter(field -> field.fieldType() != null)
                .collect(Collectors.toMap(FieldContract::fieldName, FieldContract::fieldType, (left, right) -> left));

        List<LowCodeConfigHealthItem> items = new ArrayList<>();
        for (FieldContract field : fields) {
            if (!field.moneyConfigured()) {
                continue;
            }
            String targetId = field.fieldName() == null ? "money" : field.fieldName();
            requireType(items, field.fieldType(), NUMERIC_TYPES, "MONEY_OWNER_NOT_NUMERIC",
                    "money field requires numeric owner", targetId);
            if (field.currencyMode() == null) {
                items.add(error("MONEY_CURRENCY_MODE_MISSING", "money field requires currencyMode", targetId,
                        "Set currencyMode to FIXED or SELECTABLE"));
            } else if ("FIXED".equals(field.currencyMode())) {
                requireFixedCurrencyCode(items, field.fixedCurrencyCode(), targetId);
            } else if ("SELECTABLE".equals(field.currencyMode())) {
                requireRelatedField(items, field.currencyFieldName(), fieldNames,
                        "MONEY_CURRENCY_COMPANION_MISSING", "selectable money field requires currency companion field",
                        targetId);
                requireRelatedType(items, field.currencyFieldName(), fieldTypes, TEXT_TYPES,
                        "MONEY_CURRENCY_COMPANION_NOT_TEXT",
                        "money currency companion field must be text", targetId);
            } else {
                items.add(error("MONEY_CURRENCY_MODE_INVALID", "money currencyMode must be FIXED or SELECTABLE",
                        targetId, "Set currencyMode to FIXED or SELECTABLE"));
            }
            requireCurrencyCode(items, field.defaultCurrencyCode(), "MONEY_DEFAULT_CURRENCY_INVALID",
                    "money defaultCurrencyCode must be ISO 4217 alpha-3 code", targetId);
            requireCurrencyCode(items, field.baseCurrencyCode(), "MONEY_BASE_CURRENCY_INVALID",
                    "money baseCurrencyCode must be ISO 4217 alpha-3 code", targetId);
            requireBaseAmountField(items, field, fieldNames, fieldTypes, targetId);
            requireRateTypeCode(items, field.rateTypeCode(), targetId);
            requireOptionalRelatedField(items, field.rateDateFieldName(), fieldNames,
                    "MONEY_RATE_DATE_FIELD_MISSING", "money rate date field is missing", targetId);
            requireRelatedType(items, field.rateDateFieldName(), fieldTypes, DATE_TYPES,
                    "MONEY_RATE_DATE_FIELD_NOT_DATE",
                    "money rate date field must be date or timestamp", targetId);
            requireOptionalRelatedField(items, field.exchangeRateFieldName(), fieldNames,
                    "MONEY_EXCHANGE_RATE_FIELD_MISSING", "money exchange rate field is missing", targetId);
            requireRelatedType(items, field.exchangeRateFieldName(), fieldTypes, NUMERIC_TYPES,
                    "MONEY_EXCHANGE_RATE_FIELD_NOT_NUMERIC",
                    "money exchange rate field must be numeric", targetId);
        }
        return List.copyOf(items);
    }

    private void requireBaseAmountField(List<LowCodeConfigHealthItem> items,
                                        FieldContract field,
                                        Set<String> fieldNames,
                                        Map<String, String> fieldTypes,
                                        String targetId) {
        String baseAmountFieldName = field.baseAmountFieldName();
        String ownerFieldName = field.fieldName();
        if (baseAmountFieldName == null) {
            items.add(error("MONEY_BASE_AMOUNT_MISSING",
                    "money field requires base amount field", targetId,
                    "Add a shadow base amount field and bind baseAmountFieldName"));
            return;
        }
        if (Objects.equals(ownerFieldName, baseAmountFieldName)
                || (field.ownerFieldId() != null && field.baseAmountFieldId() != null
                && Objects.equals(field.ownerFieldId(), field.baseAmountFieldId()))) {
            items.add(error("MONEY_BASE_AMOUNT_CONFLICT",
                    "money base amount field must be different from owner", targetId,
                    "Use a dedicated shadow field such as " + ownerFieldName + "Base"));
            return;
        }
        if (!fieldNames.contains(baseAmountFieldName)) {
            items.add(error("MONEY_BASE_AMOUNT_MISSING",
                    "money base amount field is missing from metadata fields", targetId,
                    "Include the shadow base amount field in the metadata bundle"));
            return;
        }
        requireRelatedType(items, baseAmountFieldName, fieldTypes, NUMERIC_TYPES,
                "MONEY_BASE_AMOUNT_NOT_NUMERIC", "money base amount field must be numeric", targetId);
    }

    private void requireRelatedField(List<LowCodeConfigHealthItem> items,
                                     String fieldName,
                                     Set<String> fieldNames,
                                     String code,
                                     String message,
                                     String targetId) {
        if (fieldName == null || !fieldNames.contains(fieldName)) {
            items.add(error(code, message, targetId, "Include and bind related field"));
        }
    }

    private void requireOptionalRelatedField(List<LowCodeConfigHealthItem> items,
                                             String fieldName,
                                             Set<String> fieldNames,
                                             String code,
                                             String message,
                                             String targetId) {
        if (fieldName != null && !fieldNames.contains(fieldName)) {
            items.add(error(code, message, targetId, "Include or remove related field"));
        }
    }

    private void requireRelatedType(List<LowCodeConfigHealthItem> items,
                                    String fieldName,
                                    Map<String, String> fieldTypes,
                                    Set<String> expectedTypes,
                                    String code,
                                    String message,
                                    String targetId) {
        if (fieldName == null) {
            return;
        }
        requireType(items, fieldTypes.get(fieldName), expectedTypes, code, message, targetId);
    }

    private void requireType(List<LowCodeConfigHealthItem> items,
                             String fieldType,
                             Set<String> expectedTypes,
                             String code,
                             String message,
                             String targetId) {
        if (fieldType != null && !expectedTypes.contains(fieldType)) {
            items.add(error(code, message, targetId, "Use a compatible metadata field type"));
        }
    }

    private void requireFixedCurrencyCode(List<LowCodeConfigHealthItem> items, String value, String targetId) {
        if (value == null) {
            items.add(error("MONEY_FIXED_CURRENCY_MISSING", "fixed money field requires fixedCurrencyCode",
                    targetId, "Set fixedCurrencyCode"));
            return;
        }
        if (!value.toUpperCase().matches("[A-Z]{3}")) {
            items.add(error("MONEY_FIXED_CURRENCY_INVALID",
                    "money fixedCurrencyCode must be ISO 4217 alpha-3 code",
                    targetId, "Use ISO 4217 alpha-3 currency code"));
        }
    }

    private void requireCurrencyCode(List<LowCodeConfigHealthItem> items,
                                     String value,
                                     String code,
                                     String message,
                                     String targetId) {
        if (value != null && !value.toUpperCase().matches("[A-Z]{3}")) {
            items.add(error(code, message, targetId, "Use ISO 4217 alpha-3 currency code"));
        }
    }

    private void requireRateTypeCode(List<LowCodeConfigHealthItem> items, String value, String targetId) {
        if (value == null) {
            items.add(error("MONEY_RATE_TYPE_MISSING", "money field requires rateTypeCode", targetId,
                    "Set exchange rate type code"));
            return;
        }
        if (!value.toUpperCase().matches("[A-Z][A-Z0-9_]{0,63}")) {
            items.add(error("MONEY_RATE_TYPE_INVALID", "money rateTypeCode must use upper snake code", targetId,
                    "Use upper snake exchange rate type code"));
        }
    }

    private LowCodeConfigHealthItem error(String code, String message, String targetId, String suggestion) {
        return LowCodeConfigHealthItem.error(
                LowCodeConfigHealthScope.METADATA,
                code,
                message,
                "field",
                targetId,
                suggestion
        );
    }

    private List<Map<String, Object>> fields(Map<String, Object> content) {
        List<Map<String, Object>> fields = new ArrayList<>();
        addFields(fields, content.get("fields"));
        addFields(fields, content.get("metadataFields"));
        addFields(fields, content.get("moduleFields"));
        return List.copyOf(fields);
    }

    private List<FieldContract> contracts(List<Map<String, Object>> rawFields) {
        Map<String, String> fieldNameById = rawFields.stream()
                .filter(field -> text(field, "id") != null || text(field, "metadataFieldId") != null)
                .filter(field -> text(field, "fieldName") != null)
                .collect(Collectors.toMap(
                        this::fieldIdentity,
                        field -> text(field, "fieldName"),
                        (left, right) -> left
                ));
        return rawFields.stream()
                .map(field -> contract(field, fieldNameById))
                .toList();
    }

    private FieldContract contract(Map<String, Object> field, Map<String, String> fieldNameById) {
        Map<String, Object> money = field.get("money") instanceof Map<?, ?> map
                ? normalizeMap(map)
                : Map.of();
        String ownerFieldId = firstText(field, money, "metadataFieldId", "id");
        String baseAmountFieldId = firstText(field, money, "moneyBaseAmountFieldId", "baseAmountFieldId");
        return new FieldContract(
                firstNonBlank(text(field, "fieldName"), fieldNameById.get(ownerFieldId)),
                ownerFieldId,
                type(field),
                moneyConfigured(field, money),
                firstText(field, money, "moneyCurrencyMode", "currencyMode", "mode"),
                firstText(field, money, "moneyFixedCurrencyCode", "fixedCurrencyCode"),
                firstText(field, money, "moneyDefaultCurrencyCode", "defaultCurrencyCode"),
                relatedFieldName(field, money, fieldNameById, "moneyCurrencyFieldName", "currencyFieldName",
                        "moneyCurrencyFieldId", "currencyFieldId"),
                relatedFieldName(field, money, fieldNameById, "moneyBaseAmountFieldName", "baseAmountFieldName",
                        "moneyBaseAmountFieldId", "baseAmountFieldId"),
                baseAmountFieldId,
                firstText(field, money, "moneyBaseCurrencyCode", "baseCurrencyCode"),
                firstText(field, money, "moneyRateTypeCode", "rateTypeCode"),
                relatedFieldName(field, money, fieldNameById, "moneyRateDateFieldName", "rateDateFieldName",
                        "moneyRateDateFieldId", "rateDateFieldId"),
                relatedFieldName(field, money, fieldNameById, "moneyExchangeRateFieldName", "exchangeRateFieldName",
                        "moneyExchangeRateFieldId", "exchangeRateFieldId")
        );
    }

    private boolean moneyConfigured(Map<String, Object> field, Map<String, Object> money) {
        return firstText(field, money, "moneyCurrencyMode", "currencyMode", "mode") != null
                || firstText(field, money, "moneyFixedCurrencyCode", "fixedCurrencyCode") != null
                || firstText(field, money, "moneyDefaultCurrencyCode", "defaultCurrencyCode") != null
                || firstText(field, money, "moneyBaseAmountFieldId", "baseAmountFieldId",
                "moneyBaseAmountFieldName", "baseAmountFieldName") != null
                || firstText(field, money, "moneyBaseCurrencyCode", "baseCurrencyCode") != null
                || firstText(field, money, "moneyRateTypeCode", "rateTypeCode") != null;
    }

    private String fieldIdentity(Map<String, Object> field) {
        String id = text(field, "id");
        return id == null ? text(field, "metadataFieldId") : id;
    }

    private String relatedFieldName(Map<String, Object> field,
                                    Map<String, Object> money,
                                    Map<String, String> fieldNameById,
                                    String nameKey,
                                    String nestedNameKey,
                                    String idKey,
                                    String nestedIdKey) {
        String fieldName = firstText(field, money, nameKey, nestedNameKey);
        if (fieldName != null) {
            return fieldName;
        }
        String fieldId = firstText(field, money, idKey, nestedIdKey);
        return fieldId == null ? null : fieldNameById.get(fieldId);
    }

    private String firstText(Map<String, Object> field, Map<String, Object> money, String... keys) {
        for (String key : keys) {
            String value = text(field, key);
            if (value != null) {
                return value;
            }
            value = text(money, key);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private String firstNonBlank(String first, String second) {
        return first != null ? first : second;
    }

    private String type(Map<String, Object> field) {
        String type = firstText(field, Map.of(), "fieldType", "type");
        return type == null ? null : type.trim().toUpperCase();
    }

    private void addFields(List<Map<String, Object>> fields, Object value) {
        if (!(value instanceof List<?> list)) {
            return;
        }
        for (Object item : list) {
            if (item instanceof Map<?, ?> map) {
                fields.add(normalizeMap(map));
            }
        }
    }

    private Map<String, Object> normalizeMap(Map<?, ?> map) {
        Map<String, Object> normalized = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (entry.getKey() instanceof String key) {
                normalized.put(key, entry.getValue());
            }
        }
        return normalized;
    }

    private String text(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value == null || value.toString().isBlank() ? null : value.toString().trim();
    }

    private record FieldContract(
            String fieldName,
            String ownerFieldId,
            String fieldType,
            boolean moneyConfigured,
            String currencyMode,
            String fixedCurrencyCode,
            String defaultCurrencyCode,
            String currencyFieldName,
            String baseAmountFieldName,
            String baseAmountFieldId,
            String baseCurrencyCode,
            String rateTypeCode,
            String rateDateFieldName,
            String exchangeRateFieldName
    ) {
    }
}
