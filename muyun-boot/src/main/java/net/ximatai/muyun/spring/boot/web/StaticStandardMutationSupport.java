package net.ximatai.muyun.spring.boot.web;

import net.ximatai.muyun.spring.ability.CrudAbility;
import net.ximatai.muyun.spring.ability.DataScopeAbility;
import net.ximatai.muyun.spring.common.model.contract.EntityContract;
import net.ximatai.muyun.spring.common.platform.ActionExecutionContext;
import net.ximatai.muyun.spring.common.platform.ActionExecutionContextHolder;
import net.ximatai.muyun.spring.common.platform.ActionExecutionPolicy;
import net.ximatai.muyun.spring.common.platform.PlatformAction;

import java.util.List;
import java.util.function.IntSupplier;

final class StaticStandardMutationSupport {
    private StaticStandardMutationSupport() {
    }

    static <T extends EntityContract> T selectForAction(ScopedWeb<? extends CrudAbility<T>> web,
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

    static void requireDataScopeRecord(ScopedWeb<? extends CrudAbility<?>> web,
                                       PlatformAction action,
                                       String id) {
        CrudAbility<?> service = web.service();
        if (service instanceof DataScopeAbility<?>) {
            DataScopeAbility<?> dataScopeAbility = DataScopeAbility.cast(service);
            dataScopeAbility.requireRecordScope(actionPolicy(web, action), List.of(id));
        }
    }

    static void created(ScopedWeb<?> web, String id) {
        StaticCrudActionResultSupport.created(web.webScopeName(), id);
    }

    static void updated(ScopedWeb<?> web, String id) {
        StaticCrudActionResultSupport.updated(web.webScopeName(), id);
    }

    static int enabled(ScopedWeb<?> web, String id, IntSupplier action) {
        return countMutation(id, action,
                recordId -> StaticCrudActionResultSupport.enabled(web.webScopeName(), recordId));
    }

    static int disabled(ScopedWeb<?> web, String id, IntSupplier action) {
        return countMutation(id, action,
                recordId -> StaticCrudActionResultSupport.disabled(web.webScopeName(), recordId));
    }

    static int deleted(ScopedWeb<?> web, String id, IntSupplier action) {
        return countMutation(id, action,
                recordId -> StaticCrudActionResultSupport.deleted(web.webScopeName(), recordId));
    }

    static int sorted(ScopedWeb<?> web, IntSupplier action) {
        int count = action.getAsInt();
        if (count > 0) {
            StaticCrudActionResultSupport.sorted(web.webScopeName());
        }
        return count;
    }

    private static int countMutation(String id,
                                     IntSupplier action,
                                     java.util.function.Consumer<String> successReporter) {
        int count = action.getAsInt();
        if (count > 0) {
            successReporter.accept(id);
        }
        return count;
    }

    static ActionExecutionPolicy actionPolicy(ScopedWeb<?> web, PlatformAction fallback) {
        return ActionExecutionContextHolder.current()
                .filter(context -> context.moduleAlias().equals(web.webScopeName()))
                .map(ActionExecutionContext::actionPolicy)
                .orElseGet(fallback::executionPolicy);
    }
}
