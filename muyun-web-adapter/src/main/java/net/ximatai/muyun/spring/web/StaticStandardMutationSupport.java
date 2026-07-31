package net.ximatai.muyun.spring.web;

import net.ximatai.muyun.spring.ability.CrudAbility;
import net.ximatai.muyun.spring.ability.DataScopeAbility;
import net.ximatai.muyun.spring.common.model.contract.EntityContract;
import net.ximatai.muyun.spring.common.platform.ActionExecutionContext;
import net.ximatai.muyun.spring.common.platform.ActionExecutionContextHolder;
import net.ximatai.muyun.spring.common.platform.ActionExecutionPolicy;
import net.ximatai.muyun.spring.common.platform.PlatformAction;

import java.util.List;

public final class StaticStandardMutationSupport {
    private StaticStandardMutationSupport() {
    }

    public static <T extends EntityContract> T selectForAction(ScopedWeb<? extends CrudAbility<T>> web,
                                                        PlatformAction action,
                                                        String id) {
        CrudAbility<T> service = web.service();
        if (service instanceof DataScopeAbility<?>) {
            DataScopeAbility<?> dataScopeAbility = DataScopeAbility.cast(service);
            @SuppressWarnings("unchecked")
            T record = (T) dataScopeAbility.selectForAction(action, id);
            return record;
        }
        return service.select(id);
    }

    public static void requireDataScopeRecord(ScopedWeb<? extends CrudAbility<?>> web,
                                       PlatformAction action,
                                       String id) {
        CrudAbility<?> service = web.service();
        if (service instanceof DataScopeAbility<?>) {
            DataScopeAbility<?> dataScopeAbility = DataScopeAbility.cast(service);
            dataScopeAbility.requireRecordScope(actionPolicy(web, action), List.of(id));
        }
    }

    public static void created(ScopedWeb<?> web, String id) {
        StandardMutationResultSupport.created(web, id);
    }

    public static void updated(ScopedWeb<?> web, String id) {
        StandardMutationResultSupport.updated(web, id);
    }

    public static int enabled(ScopedWeb<?> web, String id, java.util.function.IntSupplier action) {
        return StandardMutationResultSupport.enabled(web, id, action);
    }

    public static int disabled(ScopedWeb<?> web, String id, java.util.function.IntSupplier action) {
        return StandardMutationResultSupport.disabled(web, id, action);
    }

    public static int deleted(ScopedWeb<?> web, String id, java.util.function.IntSupplier action) {
        return StandardMutationResultSupport.deleted(web, id, action);
    }

    public static int sorted(ScopedWeb<?> web, java.util.function.IntSupplier action) {
        return StandardMutationResultSupport.sorted(web, action);
    }

    public static ActionExecutionPolicy actionPolicy(ScopedWeb<?> web, PlatformAction fallback) {
        return ActionExecutionContextHolder.current()
                .filter(context -> context.moduleAlias().equals(web.webScopeName()))
                .map(ActionExecutionContext::actionPolicy)
                .orElseGet(fallback::executionPolicy);
    }
}
