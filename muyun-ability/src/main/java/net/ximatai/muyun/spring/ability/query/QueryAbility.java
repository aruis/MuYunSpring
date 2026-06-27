package net.ximatai.muyun.spring.ability.query;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.Sort;
import net.ximatai.muyun.spring.common.model.contract.EntityContract;

public interface QueryAbility<T extends EntityContract> {
    QueryDescriptor queryDescriptor();

    default QuerySchema querySchema() {
        QueryDescriptor descriptor = queryDescriptor();
        if (descriptor == null) {
            return QuerySchema.EMPTY;
        }
        return QuerySchema.from(descriptor);
    }

    default Criteria queryCriteria(QueryRequest request) {
        QueryDescriptor descriptor = queryDescriptor();
        if (descriptor == null) {
            return Criteria.of();
        }
        return new QueryCompiler(descriptor).criteria(request);
    }

    default Sort[] querySorts(QueryRequest request) {
        QueryDescriptor descriptor = queryDescriptor();
        if (descriptor == null) {
            return new Sort[0];
        }
        return new QueryCompiler(descriptor).sorts(request);
    }
}
