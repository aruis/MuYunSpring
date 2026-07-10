package net.ximatai.muyun.spring.ability.query;

import net.ximatai.muyun.spring.common.option.OptionBinding;
import net.ximatai.muyun.spring.common.option.OptionFieldDefinition;
import net.ximatai.muyun.spring.common.option.OptionSelectionMode;

import java.util.EnumSet;
import java.util.Set;

public record QueryField(String fieldName,
                         String title,
                         QueryValueType valueType,
                         Set<QueryOperator> operators,
                         QueryOperator defaultOperator,
                         boolean sortable,
                         boolean quickSearch,
                         OptionBinding optionBinding,
                         OptionSelectionMode selectionMode,
                         String optionTitleField) {
    public QueryField {
        if (fieldName == null || fieldName.isBlank()) {
            throw new IllegalArgumentException("query field name must not be blank");
        }
        title = title == null || title.isBlank() ? fieldName : title.trim();
        valueType = valueType == null ? QueryValueType.STRING : valueType;
        operators = operators == null
                ? EnumSet.of(QueryOperator.EQ)
                : operators.isEmpty() ? Set.of() : EnumSet.copyOf(operators);
        defaultOperator = defaultOperator == null && !operators.isEmpty()
                ? fallbackDefaultOperator(valueType, operators)
                : defaultOperator;
        if (defaultOperator != null && !operators.contains(defaultOperator)) {
            throw new IllegalArgumentException("default query operator must be allowed: "
                    + fieldName + "." + defaultOperator);
        }
        selectionMode = optionBinding == null ? null
                : selectionMode == null ? OptionSelectionMode.SINGLE : selectionMode;
        optionTitleField = optionBinding == null || optionTitleField == null || optionTitleField.isBlank()
                ? null : optionTitleField.trim();
    }

    public static QueryField of(String fieldName, QueryOperator first, QueryOperator... rest) {
        return of(fieldName, QueryValueType.STRING, first, rest);
    }

    public static QueryField of(String fieldName, QueryValueType valueType, QueryOperator first,
                                QueryOperator... rest) {
        EnumSet<QueryOperator> operators = EnumSet.of(first, rest);
        return new QueryField(fieldName, null, valueType, operators, null, false, false, null, null, null);
    }

    public QueryField withTitle(String title) {
        return new QueryField(fieldName, title, valueType, operators, defaultOperator, sortable, quickSearch,
                optionBinding, selectionMode, optionTitleField);
    }

    public QueryField withDefaultOperator(QueryOperator operator) {
        return new QueryField(fieldName, title, valueType, operators, operator, sortable, quickSearch,
                optionBinding, selectionMode, optionTitleField);
    }

    public QueryField withSortable() {
        return new QueryField(fieldName, title, valueType, operators, defaultOperator, true, quickSearch,
                optionBinding, selectionMode, optionTitleField);
    }

    public QueryField withQuickSearch() {
        return new QueryField(fieldName, title, valueType, operators, defaultOperator, sortable, true,
                optionBinding, selectionMode, optionTitleField);
    }

    public QueryField withOptionBinding(OptionBinding binding) {
        return withOptionBinding(binding, OptionSelectionMode.SINGLE);
    }

    public QueryField withOptionBinding(OptionBinding binding, OptionSelectionMode selectionMode) {
        return new QueryField(fieldName, title, valueType, operators, defaultOperator, sortable, quickSearch,
                binding, selectionMode, null);
    }

    public QueryField withOptionField(OptionFieldDefinition definition) {
        if (definition == null) {
            throw new IllegalArgumentException("option field definition must not be null");
        }
        return new QueryField(fieldName, title, valueType, operators, defaultOperator, sortable, quickSearch,
                definition.binding(), definition.selectionMode(),
                definition.hasTitleOutput() ? definition.titleOutputField() : null);
    }

    private static QueryOperator fallbackDefaultOperator(QueryValueType valueType, Set<QueryOperator> operators) {
        QueryOperator typeDefault = valueType.defaultOperator();
        return operators.contains(typeDefault) ? typeDefault : operators.iterator().next();
    }
}
