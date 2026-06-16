package net.ximatai.muyun.spring.platform.config;

import net.ximatai.muyun.spring.platform.currency.CurrencyCodeRules;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
        List<FieldContract> fields = LowCodeMetadataFieldProjection.from(bundle).stream()
                .map(this::contract)
                .toList();
        if (fields.isEmpty()) {
            return List.of();
        }
        Set<String> fieldNames = fields.stream()
                .map(FieldContract::fieldName)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Set<String> currencyDependencies = dependencies(modulePackage, LowCodePackageDependencyType.CURRENCY);
        Set<String> rateTypeDependencies = dependencies(modulePackage, LowCodePackageDependencyType.EXCHANGE_RATE_TYPE);
        Map<String, String> fieldTypes = fields.stream()
                .filter(field -> field.fieldName() != null)
                .filter(field -> field.fieldType() != null)
                .collect(Collectors.toMap(FieldContract::fieldName, FieldContract::fieldType, (left, right) -> left));

        List<LowCodeConfigHealthItem> items = new ArrayList<>();
        Set<String> reportedCurrencyDependencies = new HashSet<>();
        Set<String> reportedRateTypeDependencies = new HashSet<>();
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
            requireCurrencyDependencies(items, field, currencyDependencies, reportedCurrencyDependencies);
            requireRateTypeDependency(items, field.rateTypeCode(), rateTypeDependencies, reportedRateTypeDependencies);
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

    private Set<String> dependencies(LowCodeModulePackage modulePackage, LowCodePackageDependencyType type) {
        return modulePackage.dependencyManifest().dependencies().stream()
                .filter(Objects::nonNull)
                .filter(dependency -> dependency.type() == type)
                .map(LowCodePackageDependency::alias)
                .filter(Objects::nonNull)
                .map(String::toUpperCase)
                .collect(Collectors.toSet());
    }

    private void requireCurrencyDependencies(List<LowCodeConfigHealthItem> items,
                                             FieldContract field,
                                             Set<String> dependencies,
                                             Set<String> reportedDependencies) {
        Set<String> requiredCodes = Stream.of(
                        field.fixedCurrencyCode(),
                        field.defaultCurrencyCode(),
                        field.baseCurrencyCode()
                )
                .filter(Objects::nonNull)
                .map(CurrencyCodeRules::normalizeCurrencyCode)
                .filter(CurrencyCodeRules::isCurrencyCode)
                .collect(Collectors.toSet());
        for (String currencyCode : requiredCodes) {
            if (!dependencies.contains(currencyCode) && reportedDependencies.add(currencyCode)) {
                items.add(warn("MONEY_CURRENCY_DEPENDENCY_MISSING",
                        "money currency is not declared in dependency manifest",
                        "currency",
                        currencyCode,
                        "Declare CURRENCY dependency " + currencyCode + " for cross-environment migration"));
            }
        }
    }

    private void requireRateTypeDependency(List<LowCodeConfigHealthItem> items,
                                           String rateTypeCode,
                                           Set<String> dependencies,
                                           Set<String> reportedDependencies) {
        if (rateTypeCode == null) {
            return;
        }
        String normalized = CurrencyCodeRules.normalizeRateTypeCode(rateTypeCode);
        if (CurrencyCodeRules.isRateTypeCode(normalized) && !dependencies.contains(normalized)
                && reportedDependencies.add(normalized)) {
            items.add(warn("MONEY_RATE_TYPE_DEPENDENCY_MISSING",
                    "money exchange rate type is not declared in dependency manifest",
                    "exchangeRateType",
                    normalized,
                    "Declare EXCHANGE_RATE_TYPE dependency " + normalized + " for cross-environment migration"));
        }
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
        if (!CurrencyCodeRules.isCurrencyCode(value)) {
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
        if (value != null && !CurrencyCodeRules.isCurrencyCode(value)) {
            items.add(error(code, message, targetId, "Use ISO 4217 alpha-3 currency code"));
        }
    }

    private void requireRateTypeCode(List<LowCodeConfigHealthItem> items, String value, String targetId) {
        if (value == null) {
            items.add(error("MONEY_RATE_TYPE_MISSING", "money field requires rateTypeCode", targetId,
                    "Set exchange rate type code"));
            return;
        }
        if (!CurrencyCodeRules.isRateTypeCode(value)) {
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

    private LowCodeConfigHealthItem warn(String code,
                                         String message,
                                         String targetType,
                                         String targetId,
                                         String suggestion) {
        return LowCodeConfigHealthItem.warn(
                LowCodeConfigHealthScope.DEPENDENCY,
                code,
                message,
                targetType,
                targetId,
                suggestion
        );
    }

    private FieldContract contract(LowCodeMetadataFieldProjection field) {
        Map<String, Object> money = field.nested("money");
        String baseAmountFieldId = field.firstText(money, "moneyBaseAmountFieldId", "baseAmountFieldId");
        return new FieldContract(
                field.fieldName(),
                field.ownerFieldId(),
                field.runtimeFieldType(),
                moneyConfigured(field, money),
                field.firstText(money, "moneyCurrencyMode", "currencyMode", "mode"),
                field.firstText(money, "moneyFixedCurrencyCode", "fixedCurrencyCode"),
                field.firstText(money, "moneyDefaultCurrencyCode", "defaultCurrencyCode"),
                field.relatedFieldName(money, "moneyCurrencyFieldName", "currencyFieldName",
                        "moneyCurrencyFieldId", "currencyFieldId"),
                field.relatedFieldName(money, "moneyBaseAmountFieldName", "baseAmountFieldName",
                        "moneyBaseAmountFieldId", "baseAmountFieldId"),
                baseAmountFieldId,
                field.firstText(money, "moneyBaseCurrencyCode", "baseCurrencyCode"),
                field.firstText(money, "moneyRateTypeCode", "rateTypeCode"),
                field.relatedFieldName(money, "moneyRateDateFieldName", "rateDateFieldName",
                        "moneyRateDateFieldId", "rateDateFieldId"),
                field.relatedFieldName(money, "moneyExchangeRateFieldName", "exchangeRateFieldName",
                        "moneyExchangeRateFieldId", "exchangeRateFieldId")
        );
    }

    private boolean moneyConfigured(LowCodeMetadataFieldProjection field, Map<String, Object> money) {
        return field.firstText(money, "moneyCurrencyMode", "currencyMode", "mode") != null
                || field.firstText(money, "moneyFixedCurrencyCode", "fixedCurrencyCode") != null
                || field.firstText(money, "moneyDefaultCurrencyCode", "defaultCurrencyCode") != null
                || field.firstText(money, "moneyBaseAmountFieldId", "baseAmountFieldId",
                "moneyBaseAmountFieldName", "baseAmountFieldName") != null
                || field.firstText(money, "moneyBaseCurrencyCode", "baseCurrencyCode") != null
                || field.firstText(money, "moneyRateTypeCode", "rateTypeCode") != null;
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
