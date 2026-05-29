package net.ximatai.muyun.spring.dynamic.descriptor;

public record DynamicActionDescriptor(
        String code,
        DynamicActionKind kind,
        String title
) {
}
