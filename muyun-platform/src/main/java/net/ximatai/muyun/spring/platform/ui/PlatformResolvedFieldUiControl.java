package net.ximatai.muyun.spring.platform.ui;

import net.ximatai.muyun.spring.dynamic.metadata.ViewControlType;
import net.ximatai.muyun.spring.platform.metadata.FieldUiControlValueShape;

import java.util.List;

public record PlatformResolvedFieldUiControl(
        String alias,
        String title,
        String defaultFieldSpecAlias,
        FieldUiControlValueShape valueShape,
        String primaryValueKey,
        ViewControlType rendererType,
        String icon,
        List<PlatformResolvedFieldUiControlProperty> properties,
        List<PlatformResolvedFieldUiControlBinding> bindings
) {
    public PlatformResolvedFieldUiControl {
        properties = properties == null ? List.of() : List.copyOf(properties);
        bindings = bindings == null ? List.of() : List.copyOf(bindings);
    }
}
