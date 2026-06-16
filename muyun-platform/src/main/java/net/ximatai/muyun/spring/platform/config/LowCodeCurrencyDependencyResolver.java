package net.ximatai.muyun.spring.platform.config;

import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.platform.currency.CurrencyService;
import org.springframework.stereotype.Component;

@Component
public class LowCodeCurrencyDependencyResolver implements LowCodePackageDependencyResolver {
    private final CurrencyService currencyService;

    public LowCodeCurrencyDependencyResolver(CurrencyService currencyService) {
        this.currencyService = currencyService;
    }

    @Override
    public boolean supports(LowCodePackageDependencyType type) {
        return type == LowCodePackageDependencyType.CURRENCY;
    }

    @Override
    public boolean exists(LowCodePackageDependency dependency) {
        try {
            return currencyService.resolveCurrency(dependency.alias()) != null;
        } catch (PlatformException exception) {
            return false;
        }
    }
}
