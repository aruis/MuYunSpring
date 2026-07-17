package net.ximatai.muyun.spring.boot.platform;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public record ProjectionQueryDescriptor(String moduleAlias,
                                        String viewCode,
                                        boolean supported,
                                        ProjectionQueryFallbackReason fallbackReason,
                                        Set<String> outputFields,
                                        Set<String> internalReadFields,
                                        Set<String> queryableFields,
                                        Set<String> sortableFields,
                                        Set<String> responseFields,
                                        List<ViewFieldRef> relationOutputFields) {
    public ProjectionQueryDescriptor {
        fallbackReason = fallbackReason == null ? ProjectionQueryFallbackReason.NONE : fallbackReason;
        outputFields = copy(outputFields);
        internalReadFields = copy(internalReadFields);
        queryableFields = copy(queryableFields);
        sortableFields = copy(sortableFields);
        responseFields = copy(responseFields);
        relationOutputFields = relationOutputFields == null ? List.of() : List.copyOf(relationOutputFields);
    }

    public static ProjectionQueryDescriptor unsupported(String moduleAlias,
                                                        String viewCode,
                                                        Set<String> outputFields,
                                                        ProjectionQueryFallbackReason fallbackReason) {
        return new ProjectionQueryDescriptor(
                moduleAlias,
                viewCode,
                false,
                fallbackReason,
                outputFields,
                Set.of(),
                Set.of(),
                Set.of(),
                Set.of(),
                List.of()
        );
    }

    public static ProjectionQueryDescriptor unsupported(RecordReadProjection projection,
                                                        ProjectionQueryFallbackReason fallbackReason) {
        return unsupported(
                projection == null ? null : projection.moduleAlias(),
                projection == null ? null : projection.viewCode(),
                projection == null ? Set.of() : outputFieldNames(projection),
                fallbackReason
        );
    }

    public static ProjectionQueryDescriptor supported(RecordReadProjection projection,
                                                      RelationProjectionSqlPlan plan) {
        return new ProjectionQueryDescriptor(
                projection.moduleAlias(),
                projection.viewCode(),
                true,
                ProjectionQueryFallbackReason.NONE,
                outputFieldNames(projection),
                Set.copyOf(projection.internalReadFields()),
                plan.queryableFields(),
                plan.sortableFields(),
                plan.responseFields(),
                plan.relationOutputFields()
        );
    }

    public boolean canSort(String fieldName) {
        return fieldName != null && sortableFields.contains(fieldName);
    }

    public boolean canQuery(String fieldName) {
        return fieldName != null && queryableFields.contains(fieldName);
    }

    private static Set<String> outputFieldNames(RecordReadProjection projection) {
        LinkedHashSet<String> fields = new LinkedHashSet<>();
        projection.outputFields().stream()
                .map(ViewFieldRef::fieldName)
                .forEach(fields::add);
        return Set.copyOf(fields);
    }

    private static Set<String> copy(Set<String> fields) {
        return fields == null ? Set.of() : Set.copyOf(fields);
    }
}
