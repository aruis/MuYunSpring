package net.ximatai.muyun.spring.common.option;

import net.ximatai.muyun.spring.common.util.Preconditions;

public record OptionItem(
        String code,
        String title,
        boolean enabled,
        Integer sortOrder,
        String parentCode
) {
    public OptionItem {
        code = Preconditions.requireText(code, "code");
        title = Preconditions.requireText(title, "title");
    }
}
