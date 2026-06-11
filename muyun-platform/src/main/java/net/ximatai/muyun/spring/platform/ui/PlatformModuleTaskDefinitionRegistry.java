package net.ximatai.muyun.spring.platform.ui;

import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class PlatformModuleTaskDefinitionRegistry {
    private final CopyOnWriteArrayList<PlatformModuleTaskDefinition> definitions = new CopyOnWriteArrayList<>();

    public List<PlatformModuleTaskDefinition> listEnabled(String moduleAlias) {
        return definitions.stream()
                .filter(PlatformModuleTaskDefinition::enabled)
                .filter(definition -> definition.moduleAlias().equals(moduleAlias))
                .sorted(Comparator.comparing(PlatformModuleTaskDefinition::sortOrder)
                        .thenComparing(PlatformModuleTaskDefinition::taskCode))
                .toList();
    }

    public synchronized void register(PlatformModuleTaskDefinition definition) {
        if (definition == null) {
            return;
        }
        definitions.removeIf(existing -> existing.moduleAlias().equals(definition.moduleAlias())
                && existing.taskCode().equals(definition.taskCode()));
        definitions.add(definition);
    }

    public synchronized void replace(String moduleAlias, List<PlatformModuleTaskDefinition> moduleDefinitions) {
        if (moduleDefinitions != null) {
            moduleDefinitions.stream()
                    .filter(definition -> definition != null && !Objects.equals(definition.moduleAlias(), moduleAlias))
                    .findFirst()
                    .ifPresent(definition -> {
                        throw new IllegalArgumentException("Module task definition does not belong to module: "
                                + moduleAlias);
                    });
        }
        definitions.removeIf(existing -> existing.moduleAlias().equals(moduleAlias));
        if (moduleDefinitions != null) {
            moduleDefinitions.forEach(this::register);
        }
    }

    public synchronized void replaceManagedSource(String moduleAlias,
                                                  PlatformModuleTaskOriginType originType,
                                                  String originId,
                                                  List<PlatformModuleTaskDefinition> managedDefinitions) {
        String validModuleAlias = requireText(moduleAlias, "moduleAlias");
        if (originType == null) {
            throw new IllegalArgumentException("originType must not be null");
        }
        String validOriginId = requireText(originId, "originId");
        List<PlatformModuleTaskDefinition> incoming = managedDefinitions == null
                ? List.of()
                : managedDefinitions.stream()
                .filter(Objects::nonNull)
                .toList();
        incoming.forEach(definition -> validateManagedSourceDefinition(
                validModuleAlias, originType, validOriginId, definition));
        rejectDuplicateIncomingTaskCodes(incoming);
        incoming.forEach(definition -> definitions.stream()
                .filter(existing -> sameModuleAndCode(existing, definition))
                .filter(existing -> !sameManagedOrigin(existing, originType, validOriginId))
                .findFirst()
                .ifPresent(existing -> {
                    throw new IllegalArgumentException("Module task definition is already owned by another source: "
                            + definition.taskCode());
                }));
        definitions.removeIf(existing -> Objects.equals(existing.moduleAlias(), validModuleAlias)
                && sameManagedOrigin(existing, originType, validOriginId));
        incoming.forEach(definitions::add);
    }

    public synchronized void clear() {
        definitions.clear();
    }

    private void validateManagedSourceDefinition(String moduleAlias,
                                                 PlatformModuleTaskOriginType originType,
                                                 String originId,
                                                 PlatformModuleTaskDefinition definition) {
        if (!Objects.equals(definition.moduleAlias(), moduleAlias)) {
            throw new IllegalArgumentException("Module task definition does not belong to module: " + moduleAlias);
        }
        if (!definition.managed()) {
            throw new IllegalArgumentException("Managed source sync only accepts managed task definitions: "
                    + definition.taskCode());
        }
        if (definition.originType() != originType || !Objects.equals(definition.originId(), originId)) {
            throw new IllegalArgumentException("Managed source sync only accepts matching origin definitions: "
                    + definition.taskCode());
        }
    }

    private boolean sameModuleAndCode(PlatformModuleTaskDefinition left, PlatformModuleTaskDefinition right) {
        return Objects.equals(left.moduleAlias(), right.moduleAlias())
                && Objects.equals(left.taskCode(), right.taskCode());
    }

    private void rejectDuplicateIncomingTaskCodes(List<PlatformModuleTaskDefinition> incoming) {
        java.util.LinkedHashSet<String> taskCodes = new java.util.LinkedHashSet<>();
        incoming.stream()
                .map(PlatformModuleTaskDefinition::taskCode)
                .filter(taskCode -> !taskCodes.add(taskCode))
                .findFirst()
                .ifPresent(taskCode -> {
                    throw new IllegalArgumentException("Managed source sync contains duplicate taskCode: "
                            + taskCode);
                });
    }

    private boolean sameManagedOrigin(PlatformModuleTaskDefinition definition,
                                      PlatformModuleTaskOriginType originType,
                                      String originId) {
        return definition.managed()
                && definition.originType() == originType
                && Objects.equals(definition.originId(), originId);
    }

    private String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value.trim();
    }
}
