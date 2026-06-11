package net.ximatai.muyun.spring.platform.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.common.util.PlatformNameRules;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class LowCodeModulePackageExchangeService {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    private final LowCodeModuleConfigVersionService versionService;
    private final LowCodeModuleHealthService healthService;
    private final List<LowCodePackageDependencyResolver> dependencyResolvers;

    public LowCodeModulePackageExchangeService(LowCodeModuleConfigVersionService versionService,
                                               LowCodeModuleHealthService healthService) {
        this(versionService, healthService, List.of());
    }

    @Autowired
    public LowCodeModulePackageExchangeService(LowCodeModuleConfigVersionService versionService,
                                               LowCodeModuleHealthService healthService,
                                               List<LowCodePackageDependencyResolver> dependencyResolvers) {
        this.versionService = versionService;
        this.healthService = healthService;
        this.dependencyResolvers = dependencyResolvers == null ? List.of() : List.copyOf(dependencyResolvers);
    }

    public String exportCurrentPackage(String moduleAlias) {
        String validModuleAlias = PlatformNameRules.requireModuleAlias(moduleAlias);
        LowCodeModuleConfigVersion current = versionService.currentVersion(validModuleAlias);
        if (current == null) {
            throw new PlatformException("current low code config version not found: " + validModuleAlias);
        }
        return current.getPackageSnapshotText();
    }

    public String exportVersionPackage(String versionId) {
        LowCodeModuleConfigVersion version = versionService.select(versionId);
        if (version == null) {
            throw new PlatformException("low code config version not found: " + versionId);
        }
        return version.getPackageSnapshotText();
    }

    public LowCodeModulePackage parsePackage(String packageJson) {
        if (packageJson == null || packageJson.isBlank()) {
            throw new PlatformException("low code module package json must not be blank");
        }
        try {
            return OBJECT_MAPPER.readValue(packageJson, LowCodeModulePackage.class);
        } catch (JsonProcessingException exception) {
            throw new PlatformException("low code module package json cannot be decoded");
        }
    }

    public LowCodePackageDryRunResult dryRunImport(String packageJson) {
        return dryRunImport(parsePackage(packageJson));
    }

    public LowCodePackageDryRunResult dryRunImport(LowCodeModulePackage modulePackage) {
        LowCodeConfigHealthReport healthReport = healthService.check(LowCodeModuleHealthContext.ofPackage(modulePackage));
        List<LowCodePackageImportConflict> conflicts = conflicts(modulePackage);
        return new LowCodePackageDryRunResult(modulePackage, null, healthReport, conflicts);
    }

    private List<LowCodePackageImportConflict> conflicts(LowCodeModulePackage modulePackage) {
        List<LowCodePackageImportConflict> conflicts = new ArrayList<>();
        LowCodeModuleConfigVersion current = versionService.currentVersion(modulePackage.moduleAlias());
        if (modulePackage.mode() == LowCodePackageMode.PAGE_ONLY && current == null) {
            conflicts.add(new LowCodePackageImportConflict(
                    LowCodePackageConflictType.PAGE_ONLY_REQUIRES_EXISTING_MODULE,
                    LowCodePackageConflictSeverity.ERROR,
                    modulePackage.moduleAlias(),
                    null,
                    "PAGE_ONLY package requires an existing module config version"
            ));
        }
        if (modulePackage.mode() == LowCodePackageMode.MODULE_FULL && current != null) {
            conflicts.add(new LowCodePackageImportConflict(
                    LowCodePackageConflictType.MODULE_ALREADY_HAS_VERSION,
                    LowCodePackageConflictSeverity.WARN,
                    modulePackage.moduleAlias(),
                    current.getId(),
                    "MODULE_FULL package will be imported as draft over an existing module"
            ));
        }
        if (modulePackage.mode() == LowCodePackageMode.TEMPLATE && current != null) {
            conflicts.add(new LowCodePackageImportConflict(
                    LowCodePackageConflictType.TEMPLATE_TARGET_MODULE_EXISTS,
                    LowCodePackageConflictSeverity.ERROR,
                    modulePackage.moduleAlias(),
                    current.getId(),
                    "TEMPLATE package requires a new target module alias"
            ));
        }
        conflicts.addAll(dependencyConflicts(modulePackage));
        return conflicts;
    }

    private List<LowCodePackageImportConflict> dependencyConflicts(LowCodeModulePackage modulePackage) {
        List<LowCodePackageImportConflict> conflicts = new ArrayList<>();
        for (LowCodePackageDependency dependency : modulePackage.dependencyManifest().dependencies()) {
            LowCodePackageDependencyResolver resolver = resolver(dependency.type());
            if (resolver == null) {
                conflicts.add(dependencyConflict(modulePackage, dependency,
                        LowCodePackageConflictType.DEPENDENCY_RESOLVER_MISSING,
                        dependency.type().platformResolvedByDefault()
                                ? dependency.required()
                                : false,
                        "No dependency resolver is available for "
                                + requiredLabel(dependency) + " " + dependency.type() + " dependency"));
                continue;
            }
            if (!resolver.exists(dependency)) {
                conflicts.add(dependencyConflict(modulePackage, dependency,
                        dependency.required()
                                ? LowCodePackageConflictType.REQUIRED_DEPENDENCY_MISSING
                                : LowCodePackageConflictType.OPTIONAL_DEPENDENCY_MISSING,
                        dependency.required(),
                        "Package dependency is missing: " + dependency.type()));
            }
        }
        return conflicts;
    }

    private LowCodePackageDependencyResolver resolver(LowCodePackageDependencyType type) {
        return dependencyResolvers.stream()
                .filter(resolver -> resolver.supports(type))
                .findFirst()
                .orElse(null);
    }

    private LowCodePackageImportConflict dependencyConflict(LowCodeModulePackage modulePackage,
                                                           LowCodePackageDependency dependency,
                                                           LowCodePackageConflictType conflictType,
                                                           boolean blocking,
                                                           String message) {
        return new LowCodePackageImportConflict(
                conflictType,
                blocking ? LowCodePackageConflictSeverity.ERROR : LowCodePackageConflictSeverity.WARN,
                modulePackage.moduleAlias(),
                dependency.moduleAlias() == null ? dependency.alias() : dependency.moduleAlias(),
                message
        );
    }

    private String requiredLabel(LowCodePackageDependency dependency) {
        return dependency.required() ? "required" : "optional";
    }
}
