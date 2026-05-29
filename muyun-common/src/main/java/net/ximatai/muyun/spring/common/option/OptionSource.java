package net.ximatai.muyun.spring.common.option;

import java.util.List;

public interface OptionSource {
    OptionBinding binding();

    List<OptionItem> options(OptionQuery query);

    default List<OptionItem> options() {
        return options(OptionQuery.enabledOnly());
    }

    default OptionItem resolve(String code) {
        return options(OptionQuery.all()).stream()
                .filter(option -> option.code().equals(code))
                .findFirst()
                .orElse(null);
    }
}
