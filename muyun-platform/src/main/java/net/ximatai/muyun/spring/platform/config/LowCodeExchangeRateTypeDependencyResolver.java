package net.ximatai.muyun.spring.platform.config;

import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.platform.currency.ExchangeRateTypeService;
import jakarta.enterprise.context.Dependent;

@Dependent
public class LowCodeExchangeRateTypeDependencyResolver implements LowCodePackageDependencyResolver {
    private final ExchangeRateTypeService rateTypeService;

    public LowCodeExchangeRateTypeDependencyResolver(ExchangeRateTypeService rateTypeService) {
        this.rateTypeService = rateTypeService;
    }

    @Override
    public boolean supports(LowCodePackageDependencyType type) {
        return type == LowCodePackageDependencyType.EXCHANGE_RATE_TYPE;
    }

    @Override
    public boolean exists(LowCodePackageDependency dependency) {
        try {
            return rateTypeService.resolveRateType(dependency.alias()) != null;
        } catch (PlatformException exception) {
            return false;
        }
    }
}
