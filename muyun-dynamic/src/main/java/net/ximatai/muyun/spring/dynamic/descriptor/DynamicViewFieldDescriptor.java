package net.ximatai.muyun.spring.dynamic.descriptor;

import net.ximatai.muyun.spring.dynamic.metadata.FieldTemporalSemantics;
import net.ximatai.muyun.spring.dynamic.metadata.ViewControlType;

import java.util.List;

public record DynamicViewFieldDescriptor(
        String fieldName,
        String title,
        FieldTemporalSemantics temporalSemantics,
        boolean visible,
        ViewControlType controlType,
        String fieldUiControlAlias,
        List<DynamicFieldCompanionDescriptor> companions,
        boolean readOnly,
        boolean required,
        int columnSpan
) {
    public DynamicViewFieldDescriptor {
        temporalSemantics = temporalSemantics == null ? FieldTemporalSemantics.NONE : temporalSemantics;
        companions = companions == null ? List.of() : List.copyOf(companions);
        if (columnSpan < 1 || columnSpan > 2) {
            throw new IllegalArgumentException("columnSpan must be between 1 and 2");
        }
    }
}
