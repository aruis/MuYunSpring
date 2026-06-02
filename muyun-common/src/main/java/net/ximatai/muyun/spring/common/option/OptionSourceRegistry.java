package net.ximatai.muyun.spring.common.option;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class OptionSourceRegistry {
    private final Map<String, OptionSourceProvider> providers;

    public OptionSourceRegistry(List<OptionSourceProvider> providers) {
        Objects.requireNonNull(providers, "providers must not be null");
        this.providers = providers.stream()
                .collect(Collectors.toUnmodifiableMap(
                        provider -> Objects.requireNonNull(provider.sourceType(), "sourceType must not be null"),
                        Function.identity(),
                        (left, right) -> {
                            throw new IllegalArgumentException("duplicate option source provider: " + left.sourceType());
                        }
                ));
    }

    public OptionSource source(OptionBinding binding) {
        Objects.requireNonNull(binding, "binding must not be null");
        OptionSourceProvider provider = providers.get(binding.sourceType());
        if (provider == null) {
            throw new IllegalArgumentException("unsupported option binding: " + binding.sourceType());
        }
        return provider.source(binding);
    }
}
