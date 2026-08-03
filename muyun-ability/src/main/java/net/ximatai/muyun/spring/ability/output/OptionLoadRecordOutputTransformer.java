package net.ximatai.muyun.spring.ability.output;

import net.ximatai.muyun.spring.ability.CrudAbility;
import net.ximatai.muyun.spring.ability.option.StaticOptionLoadPopulator;
import net.ximatai.muyun.spring.common.model.contract.EntityContract;
import net.ximatai.muyun.spring.common.option.OptionLoadResolver;

import java.util.List;
import java.util.Objects;

/** Applies declared option projections to static read records. */
public class OptionLoadRecordOutputTransformer implements RecordOutputTransformer {
    private final StaticOptionLoadPopulator loadPopulator;

    public OptionLoadRecordOutputTransformer(StaticOptionLoadPopulator loadPopulator) {
        this.loadPopulator = loadPopulator == null ? StaticOptionLoadPopulator.NONE : loadPopulator;
    }

    @Override
    public boolean supports(CrudAbility<?> service, RecordOutputContext context) {
        Class<?> modelClass = service == null ? null : service.modelClass();
        return modelClass != null && !OptionLoadResolver.resolve(modelClass).isEmpty();
    }

    @Override
    public <T extends EntityContract> T transformRecord(CrudAbility<T> service,
                                                        T record,
                                                        RecordOutputContext context) {
        if (record != null) {
            loadPopulator.populate(modelClass(service, record), record);
        }
        return record;
    }

    @Override
    public <T extends EntityContract> List<T> transformRecords(CrudAbility<T> service,
                                                               List<T> records,
                                                               RecordOutputContext context) {
        if (records == null || records.isEmpty()) {
            return records;
        }
        loadPopulator.populateAll(modelClass(service, records.getFirst()), records);
        return records;
    }

    private Class<?> modelClass(CrudAbility<?> service, EntityContract fallback) {
        Class<?> modelClass = service.modelClass();
        return modelClass == null ? Objects.requireNonNull(fallback, "fallback must not be null").getClass() : modelClass;
    }
}
