package net.ximatai.muyun.spring.platform.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class LowCodeModuleDependencyHealthChecker implements LowCodeModuleHealthChecker {
    private final LowCodePackageDependencyDiagnostics dependencyDiagnostics;

    public LowCodeModuleDependencyHealthChecker() {
        this(List.of());
    }

    @Autowired
    public LowCodeModuleDependencyHealthChecker(List<LowCodePackageDependencyResolver> dependencyResolvers) {
        this.dependencyDiagnostics = new LowCodePackageDependencyDiagnostics(dependencyResolvers);
    }

    @Override
    public List<LowCodeConfigHealthItem> check(LowCodeModuleHealthContext context) {
        LowCodeModulePackage modulePackage = context == null ? null : context.modulePackage();
        if (modulePackage == null) {
            return List.of();
        }
        List<LowCodeConfigHealthItem> items = new ArrayList<>();
        for (LowCodePackageDependencyDiagnostic diagnostic : dependencyDiagnostics.diagnose(modulePackage)) {
            items.add(item(diagnostic));
        }
        return items;
    }

    private LowCodeConfigHealthItem item(LowCodePackageDependencyDiagnostic diagnostic) {
        if (diagnostic.blocking()) {
            return LowCodeConfigHealthItem.error(
                    LowCodeConfigHealthScope.DEPENDENCY,
                    diagnostic.conflictType().name(),
                    diagnostic.message(),
                    diagnostic.dependency().type().name(),
                    diagnostic.targetId(),
                    suggestion(diagnostic)
            );
        }
        return LowCodeConfigHealthItem.warn(
                LowCodeConfigHealthScope.DEPENDENCY,
                diagnostic.conflictType().name(),
                diagnostic.message(),
                diagnostic.dependency().type().name(),
                diagnostic.targetId(),
                suggestion(diagnostic)
        );
    }

    private String suggestion(LowCodePackageDependencyDiagnostic diagnostic) {
        if (diagnostic.conflictType() == LowCodePackageConflictType.DEPENDENCY_RESOLVER_MISSING) {
            return "Add a resolver or mark this dependency as manifest-only for migration diagnostics";
        }
        return "Create the dependency in the target environment or update dependencyManifest";
    }
}
