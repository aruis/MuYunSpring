package net.ximatai.muyun.spring.boot.web;

import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.database.core.orm.PageResult;
import net.ximatai.muyun.spring.ability.CrudAbility;
import net.ximatai.muyun.spring.ability.security.FieldProtectionAbility;
import net.ximatai.muyun.spring.common.model.contract.EntityContract;
import net.ximatai.muyun.spring.common.security.FieldOutputContext;

import java.util.List;

public final class WebOutputSupport {
    private WebOutputSupport() {
    }

    public static <T extends EntityContract> PageResult<T> page(CrudAbility<T> service,
                                                                PageResult<T> page,
                                                                FieldOutputContext context) {
        if (!fieldProtected(service)) {
            return page;
        }
        List<T> records = page.getRecords().stream()
                .map(record -> record(service, record, context))
                .toList();
        return PageResult.of(records, page.getTotal(), PageRequest.of(page.getPageNum(), page.getPageSize()));
    }

    public static <T extends EntityContract> List<T> records(CrudAbility<T> service,
                                                             List<T> records,
                                                             FieldOutputContext context) {
        if (!fieldProtected(service)) {
            return records;
        }
        return records.stream()
                .map(record -> record(service, record, context))
                .toList();
    }

    @SuppressWarnings("unchecked")
    public static <T extends EntityContract> T record(CrudAbility<T> service,
                                                      T record,
                                                      FieldOutputContext context) {
        if (record != null && service instanceof FieldProtectionAbility<?> fieldProtectionAbility) {
            return ((FieldProtectionAbility<T>) fieldProtectionAbility).maskProtectedFieldsForOutput(record, context);
        }
        return record;
    }

    private static boolean fieldProtected(CrudAbility<?> service) {
        return service instanceof FieldProtectionAbility<?>;
    }
}
