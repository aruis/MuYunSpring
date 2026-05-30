package net.ximatai.muyun.spring.dynamic.descriptor;

import net.ximatai.muyun.spring.dynamic.metadata.EntityViewType;

import java.util.List;

public record DynamicViewDescriptor(
        EntityViewType viewType,
        String title,
        List<DynamicViewFieldDescriptor> fields
) {
    public DynamicViewDescriptor {
        fields = fields == null ? List.of() : List.copyOf(fields);
    }
}
