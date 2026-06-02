package net.ximatai.muyun.spring.platform.dictionary;

import net.ximatai.muyun.spring.common.option.OptionBinding;
import net.ximatai.muyun.spring.common.option.OptionSource;
import net.ximatai.muyun.spring.common.option.OptionSourceProvider;
import org.springframework.stereotype.Component;

@Component
public final class DictionaryOptionSourceProvider implements OptionSourceProvider {
    private final DictionaryItemService itemService;

    public DictionaryOptionSourceProvider(DictionaryItemService itemService) {
        this.itemService = itemService;
    }

    @Override
    public boolean supports(OptionBinding binding) {
        return binding != null && OptionBinding.DICTIONARY_SOURCE.equals(binding.sourceType());
    }

    @Override
    public OptionSource source(OptionBinding binding) {
        if (!supports(binding)) {
            throw new IllegalArgumentException("unsupported dictionary option binding: " + binding);
        }
        String[] parts = binding.source().split("\\.", -1);
        if (parts.length != 2) {
            throw new IllegalArgumentException("dictionary option source must be applicationAlias.categoryAlias: "
                    + binding.source());
        }
        return new DictionaryOptionSource(parts[0], parts[1], itemService);
    }
}
