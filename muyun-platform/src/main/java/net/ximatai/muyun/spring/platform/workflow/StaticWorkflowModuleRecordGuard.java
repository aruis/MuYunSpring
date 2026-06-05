package net.ximatai.muyun.spring.platform.workflow;

import net.ximatai.muyun.spring.ability.CrudAbility;
import net.ximatai.muyun.spring.ability.DataScopeAbility;
import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.common.model.contract.EntityContract;
import net.ximatai.muyun.spring.common.platform.ActionAccessMode;
import net.ximatai.muyun.spring.common.platform.ActionDefaultGrantPolicy;
import net.ximatai.muyun.spring.common.platform.ActionExecutionPolicy;
import net.ximatai.muyun.spring.common.platform.DataScopeCriteriaResult;
import net.ximatai.muyun.spring.common.platform.PlatformActionLevel;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class StaticWorkflowModuleRecordGuard implements WorkflowModuleRecordGuard {
    public static final String SUBMIT_ACTION_CODE = "submit";
    private static final ActionExecutionPolicy SUBMIT_POLICY = new ActionExecutionPolicy(
            SUBMIT_ACTION_CODE,
            PlatformActionLevel.RECORD,
            ActionAccessMode.AUTH_REQUIRED,
            true,
            true,
            ActionDefaultGrantPolicy.NONE,
            null
    );

    private final List<CrudAbility<?>> abilities;

    public StaticWorkflowModuleRecordGuard(List<CrudAbility<?>> abilities) {
        this.abilities = abilities == null ? List.of() : List.copyOf(abilities);
    }

    @Override
    public void beforeSubmit(WorkflowSubmitRequest request) {
        Optional<CrudAbility<?>> matched = ability(request.moduleAlias());
        if (matched.isEmpty()) {
            return;
        }
        CrudAbility<?> ability = matched.get();
        EntityContract record = selectVisibleRecord(ability, request.recordId());
        if (record == null) {
            throw new PlatformException("static record not found: " + request.moduleAlias() + "." + request.recordId());
        }
    }

    private Optional<CrudAbility<?>> ability(String moduleAlias) {
        return abilities.stream()
                .filter(ability -> moduleAlias.equals(ability.getModuleAlias()))
                .findFirst();
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private EntityContract selectVisibleRecord(CrudAbility<?> ability, String recordId) {
        if (ability instanceof DataScopeAbility dataScopeAbility) {
            DataScopeCriteriaResult scope = dataScopeAbility.requireRecordScopeResult(SUBMIT_POLICY, Set.of(recordId));
            return (EntityContract) dataScopeAbility.withDataScopeTenant(scope, () -> ability.select(recordId));
        }
        return (EntityContract) ability.select(recordId);
    }
}
