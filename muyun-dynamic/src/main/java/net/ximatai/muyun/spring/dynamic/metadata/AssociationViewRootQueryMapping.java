package net.ximatai.muyun.spring.dynamic.metadata;

import java.util.List;

public record AssociationViewRootQueryMapping(
        AssociationViewQueryMappingGroupOperator groupOperator,
        List<AssociationViewRootQueryMapping> children,
        String targetField,
        DynamicQueryOperator operator,
        AssociationViewQueryMappingSourceType sourceType,
        String sourceField,
        String systemVariable,
        Object constantValue
) {
    public AssociationViewRootQueryMapping {
        groupOperator = groupOperator == null ? AssociationViewQueryMappingGroupOperator.AND : groupOperator;
        children = children == null ? List.of() : List.copyOf(children);
        operator = operator == null ? DynamicQueryOperator.EQ : operator;
    }

    public static AssociationViewRootQueryMapping sourceField(String targetField,
                                                              DynamicQueryOperator operator,
                                                              String sourceField) {
        return new AssociationViewRootQueryMapping(null, List.of(), targetField, operator,
                AssociationViewQueryMappingSourceType.SOURCE_FIELD, sourceField, null, null);
    }

    public static AssociationViewRootQueryMapping systemVariable(String targetField,
                                                                 DynamicQueryOperator operator,
                                                                 String systemVariable) {
        return new AssociationViewRootQueryMapping(null, List.of(), targetField, operator,
                AssociationViewQueryMappingSourceType.SYSTEM_VARIABLE, null, systemVariable, null);
    }

    public static AssociationViewRootQueryMapping constant(String targetField,
                                                           DynamicQueryOperator operator,
                                                           Object constantValue) {
        return new AssociationViewRootQueryMapping(null, List.of(), targetField, operator,
                AssociationViewQueryMappingSourceType.CONSTANT, null, null, constantValue);
    }

    public static AssociationViewRootQueryMapping group(AssociationViewQueryMappingGroupOperator operator,
                                                        List<AssociationViewRootQueryMapping> children) {
        return new AssociationViewRootQueryMapping(operator, children, null, null, null, null, null, null);
    }

    public boolean leaf() {
        return children.isEmpty();
    }
}
