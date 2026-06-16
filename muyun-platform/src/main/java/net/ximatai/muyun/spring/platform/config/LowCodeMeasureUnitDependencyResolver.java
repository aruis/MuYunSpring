package net.ximatai.muyun.spring.platform.config;

import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.platform.measure.MeasureUnitCategoryService;
import org.springframework.stereotype.Component;

@Component
public class LowCodeMeasureUnitDependencyResolver implements LowCodePackageDependencyResolver {
    private final MeasureUnitCategoryService categoryService;

    public LowCodeMeasureUnitDependencyResolver(MeasureUnitCategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @Override
    public boolean supports(LowCodePackageDependencyType type) {
        return type == LowCodePackageDependencyType.MEASURE_UNIT;
    }

    @Override
    public boolean exists(LowCodePackageDependency dependency) {
        try {
            categoryService.requireEnabledVisibleCategory(dependency.applicationAlias(), dependency.alias());
            return true;
        } catch (PlatformException exception) {
            return false;
        }
    }
}
