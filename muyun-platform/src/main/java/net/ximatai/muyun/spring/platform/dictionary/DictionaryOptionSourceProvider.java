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
    public String sourceType() {
        return OptionBinding.DICTIONARY_SOURCE;
    }

    @Override
    public boolean supports(OptionBinding binding) {
        return binding != null && sourceType().equals(binding.sourceType());
    }

    @Override
    public OptionSource source(OptionBinding binding) {
        if (!supports(binding)) {
            throw new IllegalArgumentException("unsupported dictionary option binding: " + binding);
        }
        OptionBinding.DictionarySource source = binding.dictionarySource();
        return new DictionaryOptionSource(source.applicationAlias(), source.categoryAlias(), itemService);
    }
}
