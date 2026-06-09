package net.ximatai.muyun.spring.platform.exchange.template;

import net.ximatai.muyun.spring.dynamic.descriptor.DynamicReferenceDescriptor;

import java.util.List;

@FunctionalInterface
public interface DynamicReferenceDropdownResolver {
    DynamicReferenceDropdownResolver NONE = (reference, limit) -> List.of();

    List<String> resolve(DynamicReferenceDescriptor reference, int limit);
}
