package net.ximatai.muyun.spring.boot.web;

import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.database.core.orm.PageResult;
import net.ximatai.muyun.spring.ability.CrudAbility;
import net.ximatai.muyun.spring.ability.option.OptionFieldOutputAbility;
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
        if (!outputTransformed(service)) {
            return page;
        }
        List<T> records = records(service, page.getRecords(), context);
        return PageResult.of(records, page.getTotal(), PageRequest.of(page.getPageNum(), page.getPageSize()));
    }

    @SuppressWarnings("unchecked")
    public static <T extends EntityContract> List<T> records(CrudAbility<T> service,
                                                             List<T> records,
                                                             FieldOutputContext context) {
        if (!outputTransformed(service)) {
            return records;
        }
        List<T> transformed = records;
        if (service instanceof OptionFieldOutputAbility<?> optionFieldOutputAbility) {
            transformed = ((OptionFieldOutputAbility<T>) optionFieldOutputAbility).populateOptionTitlesForOutput(records);
        }
        if (fieldProtected(service)) {
            return transformed.stream()
                    .map(record -> maskProtectedFieldsForOutput(service, record, context))
                    .toList();
        }
        return transformed;
    }

    @SuppressWarnings("unchecked")
    public static <T extends EntityContract> T record(CrudAbility<T> service,
                                                      T record,
                                                      FieldOutputContext context) {
        if (record != null && service instanceof OptionFieldOutputAbility<?> optionFieldOutputAbility) {
            ((OptionFieldOutputAbility<T>) optionFieldOutputAbility).populateOptionTitlesForOutput(record);
        }
        if (record != null && service instanceof FieldProtectionAbility<?> fieldProtectionAbility) {
            return ((FieldProtectionAbility<T>) fieldProtectionAbility).maskProtectedFieldsForOutput(record, context);
        }
        return record;
    }

    @SuppressWarnings("unchecked")
    private static <T extends EntityContract> T maskProtectedFieldsForOutput(CrudAbility<T> service,
                                                                             T record,
                                                                             FieldOutputContext context) {
        if (record != null && service instanceof FieldProtectionAbility<?> fieldProtectionAbility) {
            return ((FieldProtectionAbility<T>) fieldProtectionAbility).maskProtectedFieldsForOutput(record, context);
        }
        return record;
    }

    private static boolean outputTransformed(CrudAbility<?> service) {
        return fieldProtected(service) || service instanceof OptionFieldOutputAbility<?>;
    }

    private static boolean fieldProtected(CrudAbility<?> service) {
        return service instanceof FieldProtectionAbility<?>;
    }
}
