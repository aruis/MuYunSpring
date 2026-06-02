package net.ximatai.muyun.spring.common.option;

import java.util.List;
import java.util.Objects;

public final class OptionSourceRegistry {
    private final List<OptionSourceProvider> providers;

    public OptionSourceRegistry(List<OptionSourceProvider> providers) {
        this.providers = List.copyOf(Objects.requireNonNull(providers, "providers must not be null"));
    }

    public OptionSource source(OptionBinding binding) {
        Objects.requireNonNull(binding, "binding must not be null");
        return providers.stream()
                .filter(provider -> provider.supports(binding))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("unsupported option binding: " + binding.sourceType()))
                .source(binding);
    }
}
