package net.ximatai.muyun.spring.platform.web;

import net.ximatai.muyun.spring.ability.CrudAbility;
import net.ximatai.muyun.spring.ability.DataScopeAbility;
import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.common.platform.ActionExecutionPolicy;
import net.ximatai.muyun.spring.common.platform.PlatformActionLevel;
import net.ximatai.muyun.spring.common.platform.RecordActionAvailabilityContributor;
import net.ximatai.muyun.spring.common.platform.RecordActionAvailabilityDecision;
import net.ximatai.muyun.spring.common.util.PlatformNameRules;
import net.ximatai.muyun.spring.dynamic.descriptor.DynamicActionDescriptor;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicActionAvailability;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicRecord;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicRecordService;
import net.ximatai.muyun.spring.platform.module.ModuleKind;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class PlatformRecordActionAvailabilityService {
    private final PlatformModuleRuntimeContextService runtimeContextService;
    private final DynamicRecordService dynamicRecordService;
    private final PlatformDynamicModuleScopeService dynamicModuleScopeService;
    private final List<CrudAbility<?>> crudAbilities;
    private final List<RecordActionAvailabilityContributor> availabilityContributors;

    @Autowired
    public PlatformRecordActionAvailabilityService(PlatformModuleRuntimeContextService runtimeContextService,
                                                   ObjectProvider<DynamicRecordService> dynamicRecordService,
                                                   ObjectProvider<PlatformDynamicModuleScopeService> dynamicModuleScopeService,
                                                   ObjectProvider<CrudAbility<?>> crudAbilities,
                                                   ObjectProvider<RecordActionAvailabilityContributor> availabilityContributors) {
        this(runtimeContextService,
                dynamicRecordService == null ? null : dynamicRecordService.getIfAvailable(),
                dynamicModuleScopeService == null ? null : dynamicModuleScopeService.getIfAvailable(),
                crudAbilities == null ? List.of() : crudAbilities.orderedStream().toList(),
                availabilityContributors == null ? List.of() : availabilityContributors.orderedStream().toList());
    }

    PlatformRecordActionAvailabilityService(PlatformModuleRuntimeContextService runtimeContextService,
                                            DynamicRecordService dynamicRecordService,
                                            PlatformDynamicModuleScopeService dynamicModuleScopeService,
                                            List<CrudAbility<?>> crudAbilities,
                                            List<RecordActionAvailabilityContributor> availabilityContributors) {
        this.runtimeContextService = runtimeContextService;
        this.dynamicRecordService = dynamicRecordService;
        this.dynamicModuleScopeService = dynamicModuleScopeService;
        this.crudAbilities = crudAbilities == null ? List.of() : List.copyOf(crudAbilities);
        this.availabilityContributors = availabilityContributors == null
                ? List.of()
                : List.copyOf(availabilityContributors);
    }

    public PlatformRecordActionAvailability recordActions(String moduleAlias, String recordId) {
        String validModuleAlias = PlatformNameRules.requireModuleAlias(moduleAlias);
        String validRecordId = requireRecordId(recordId);
        PlatformModuleRuntimeContext context = runtimeContextService.context(validModuleAlias);
        if (context.moduleKind() == ModuleKind.DYNAMIC) {
            return dynamicRecordActions(validModuleAlias, validRecordId, context);
        }
        return staticRecordActions(validModuleAlias, validRecordId, context);
    }

    private PlatformRecordActionAvailability staticRecordActions(String moduleAlias,
                                                                 String recordId,
                                                                 PlatformModuleRuntimeContext context) {
        List<PlatformRecordActionAvailability.Action> actions = context.actions().stream()
                .filter(this::isStaticRecordAvailabilityAction)
                .map(action -> staticRecordAction(moduleAlias, recordId, action))
                .toList();
        return new PlatformRecordActionAvailability(recordId, actions);
    }

    private PlatformRecordActionAvailability dynamicRecordActions(String moduleAlias,
                                                                 String recordId,
                                                                 PlatformModuleRuntimeContext context) {
        if (dynamicRecordService == null) {
            throw new PlatformException("dynamic record service is not configured");
        }
        if (dynamicModuleScopeService == null) {
            throw new PlatformException("dynamic module scope service is not configured");
        }
        dynamicModuleScopeService.requireTenantScope(moduleAlias);
        String entityAlias = context.mainEntityAlias() == null || context.mainEntityAlias().isBlank()
                ? dynamicRecordService.mainEntityAlias(moduleAlias)
                : context.mainEntityAlias();
        Map<String, DynamicActionDescriptor> descriptors = dynamicRecordService.actions(moduleAlias).stream()
                .collect(Collectors.toMap(DynamicActionDescriptor::code, Function.identity(), (left, right) -> left));
        DynamicRecordHolder recordHolder = new DynamicRecordHolder();
        List<PlatformRecordActionAvailability.Action> actions = context.actions().stream()
                .filter(this::isDynamicRecordAvailabilityAction)
                .filter(action -> descriptors.containsKey(action.actionCode()))
                .map(action -> dynamicRecordAction(moduleAlias, entityAlias, recordId, recordHolder, action))
                .toList();
        return new PlatformRecordActionAvailability(recordId, actions);
    }

    private PlatformRecordActionAvailability.Action dynamicRecordAction(String moduleAlias,
                                                                        String entityAlias,
                                                                        String recordId,
                                                                        DynamicRecordHolder recordHolder,
                                                                        PlatformModuleRuntimeAction action) {
        if (!action.authorized()) {
            return unavailable(action.actionCode(), "no action auth");
        }
        DynamicActionAvailability authorization = dynamicRecordService.actionAuthorizationAvailability(
                moduleAlias, entityAlias, action.actionCode(), Set.of(recordId));
        if (!authorization.available()) {
            return unavailable(action.actionCode(), normalizeReason(authorization.message(), "no data auth"));
        }
        DynamicRecord record = recordHolder.record();
        if (record == null) {
            record = dynamicRecordService.select(moduleAlias, entityAlias, recordId);
            if (record == null) {
                throw new IllegalArgumentException("dynamic record does not exist: " + recordId);
            }
            recordHolder.record(record);
        }
        DynamicActionAvailability availability = dynamicRecordService.actionAvailability(moduleAlias,
                action.actionCode(), record);
        if (!availability.available()) {
            return unavailable(action.actionCode(), availability.message());
        }
        return new PlatformRecordActionAvailability.Action(action.actionCode(), true, null);
    }

    private PlatformRecordActionAvailability.Action staticRecordAction(String moduleAlias,
                                                                       String recordId,
                                                                       PlatformModuleRuntimeAction action) {
        if (!action.authorized()) {
            return unavailable(action.actionCode(), "no action auth");
        }
        ActionExecutionPolicy policy = policy(action);
        if (policy.requiresDataScope() && !hasRecordDataScope(moduleAlias, recordId, policy)) {
            return unavailable(action.actionCode(), "no data auth");
        }
        Optional<RecordActionAvailabilityDecision> businessDecision = businessAvailability(moduleAlias,
                action.actionCode(), recordId);
        if (businessDecision.isPresent() && !businessDecision.get().available()) {
            return unavailable(action.actionCode(), businessDecision.get().reason());
        }
        return new PlatformRecordActionAvailability.Action(action.actionCode(), true, null);
    }

    private boolean hasRecordDataScope(String moduleAlias, String recordId, ActionExecutionPolicy policy) {
        CrudAbility<?> ability = crudAbility(moduleAlias);
        if (ability instanceof DataScopeAbility<?> dataScopeAbility) {
            try {
                dataScopeAbility.requireRecordScope(policy, List.of(recordId));
                return true;
            } catch (PlatformException | IllegalArgumentException ignored) {
                return false;
            }
        }
        return true;
    }

    private Optional<RecordActionAvailabilityDecision> businessAvailability(String moduleAlias,
                                                                           String actionCode,
                                                                           String recordId) {
        for (RecordActionAvailabilityContributor contributor : availabilityContributors) {
            Optional<RecordActionAvailabilityDecision> decision = contributor.availability(moduleAlias, actionCode,
                    recordId);
            if (decision.isPresent()) {
                return decision;
            }
        }
        return Optional.empty();
    }

    private CrudAbility<?> crudAbility(String moduleAlias) {
        return crudAbilities.stream()
                .filter(ability -> moduleAlias.equals(ability.getModuleAlias()))
                .findFirst()
                .orElse(null);
    }

    private boolean isStaticRecordAvailabilityAction(PlatformModuleRuntimeAction action) {
        return action.actionLevel() == PlatformActionLevel.RECORD
                || action.actionLevel() == PlatformActionLevel.ANY;
    }

    private boolean isDynamicRecordAvailabilityAction(PlatformModuleRuntimeAction action) {
        return action.actionLevel() == PlatformActionLevel.RECORD
                || action.actionLevel() == PlatformActionLevel.ANY;
    }

    private ActionExecutionPolicy policy(PlatformModuleRuntimeAction action) {
        return new ActionExecutionPolicy(
                action.actionCode(),
                action.actionLevel(),
                action.accessMode(),
                action.actionAuth(),
                action.dataAuth(),
                action.defaultGrantPolicy(),
                PlatformModuleRuntimeContextService.inheritActionCode(action.actionCode(),
                        action.permissionActionCode(), action.actionAuth())
        );
    }

    private PlatformRecordActionAvailability.Action unavailable(String actionCode, String reason) {
        return new PlatformRecordActionAvailability.Action(actionCode, false, reason);
    }

    private String requireRecordId(String recordId) {
        if (recordId == null || recordId.isBlank()) {
            throw new IllegalArgumentException("recordId must not be blank");
        }
        return recordId.trim();
    }

    private String normalizeReason(String reason, String fallback) {
        return reason == null || reason.isBlank() ? fallback : reason.trim();
    }

    private static final class DynamicRecordHolder {
        private DynamicRecord record;

        private DynamicRecord record() {
            return record;
        }

        private void record(DynamicRecord record) {
            this.record = record;
        }
    }
}
