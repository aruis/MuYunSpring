package net.ximatai.muyun.spring.platform.config;

import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.platform.dictionary.DictionaryCategoryService;
import org.springframework.stereotype.Component;

@Component
public class LowCodeDictionaryDependencyResolver implements LowCodePackageDependencyResolver {
    private final DictionaryCategoryService categoryService;

    public LowCodeDictionaryDependencyResolver(DictionaryCategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @Override
    public boolean supports(LowCodePackageDependencyType type) {
        return type == LowCodePackageDependencyType.DICTIONARY;
    }

    @Override
    public boolean exists(LowCodePackageDependency dependency) {
        try {
            categoryService.requireEnabledDictionaryCategory(dependency.applicationAlias(), dependency.alias());
            return true;
        } catch (PlatformException exception) {
            return false;
        }
    }
}
