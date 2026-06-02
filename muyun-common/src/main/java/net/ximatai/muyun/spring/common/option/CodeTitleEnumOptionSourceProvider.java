package net.ximatai.muyun.spring.common.option;

import net.ximatai.muyun.spring.common.model.contract.CodeTitleEnum;

public final class CodeTitleEnumOptionSourceProvider implements OptionSourceProvider {
    @Override
    public String sourceType() {
        return OptionBinding.ENUM_SOURCE;
    }

    @Override
    public boolean supports(OptionBinding binding) {
        return binding != null && sourceType().equals(binding.sourceType());
    }

    @Override
    public OptionSource source(OptionBinding binding) {
        if (!supports(binding)) {
            throw new IllegalArgumentException("unsupported enum option binding: " + binding);
        }
        Class<?> rawType = loadType(binding.source());
        if (!rawType.isEnum() || !CodeTitleEnum.class.isAssignableFrom(rawType)) {
            throw new IllegalArgumentException("enum option binding requires CodeTitleEnum: " + binding.source());
        }
        return enumSource(rawType);
    }

    private Class<?> loadType(String className) {
        try {
            return Class.forName(className, false, Thread.currentThread().getContextClassLoader());
        } catch (ClassNotFoundException ex) {
            throw new IllegalArgumentException("unknown enum option type: " + className, ex);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private OptionSource enumSource(Class<?> rawType) {
        return CodeTitleEnumOptionSource.of((Class) rawType);
    }
}
