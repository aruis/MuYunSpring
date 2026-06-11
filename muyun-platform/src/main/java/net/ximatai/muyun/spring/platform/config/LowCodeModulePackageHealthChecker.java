package net.ximatai.muyun.spring.platform.config;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

@Component
public class LowCodeModulePackageHealthChecker implements LowCodeModuleHealthChecker {
    private final LowCodeModulePackageValidator validator;

    public LowCodeModulePackageHealthChecker(LowCodeModulePackageValidator validator) {
        this.validator = validator == null ? new LowCodeModulePackageValidator() : validator;
    }

    public LowCodeModulePackageHealthChecker() {
        this(null);
    }

    @Override
    public List<LowCodeConfigHealthItem> check(LowCodeModuleHealthContext context) {
        LowCodeModulePackage modulePackage = context == null ? null : context.modulePackage();
        if (modulePackage == null) {
            return List.of();
        }
        if (!Objects.equals(context.moduleAlias(), modulePackage.moduleAlias())) {
            return List.of(LowCodeConfigHealthItem.error(
                    LowCodeConfigHealthScope.PACKAGE,
                    "PACKAGE_MODULE_MISMATCH",
                    "package moduleAlias does not match health context",
                    "module",
                    modulePackage.moduleAlias(),
                    "Check the target module before importing or publishing this package"
            ));
        }
        try {
            validator.validate(modulePackage);
            return List.of();
        } catch (IllegalArgumentException ex) {
            return List.of(LowCodeConfigHealthItem.error(
                    LowCodeConfigHealthScope.PACKAGE,
                    "PACKAGE_INVALID",
                    ex.getMessage(),
                    "package",
                    modulePackage.moduleAlias(),
                    "Fix the package structure before running publish, migration or template operations"
            ));
        }
    }
}
