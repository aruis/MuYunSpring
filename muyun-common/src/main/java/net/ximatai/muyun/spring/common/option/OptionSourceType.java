package net.ximatai.muyun.spring.common.option;

import net.ximatai.muyun.spring.common.util.Preconditions;

public enum OptionSourceType {
    DICTIONARY(OptionBinding.DICTIONARY_SOURCE),
    ENUM(OptionBinding.ENUM_SOURCE);

    private final String bindingSourceType;

    OptionSourceType(String bindingSourceType) {
        this.bindingSourceType = bindingSourceType;
    }

    public String bindingSourceType() {
        return bindingSourceType;
    }

    public OptionBinding toBinding(String source) {
        String validSource = Preconditions.requireText(source, "optionSource");
        OptionBinding binding = new OptionBinding(bindingSourceType, validSource);
        if (this == DICTIONARY) {
            binding.dictionarySource();
        }
        return binding;
    }
}
