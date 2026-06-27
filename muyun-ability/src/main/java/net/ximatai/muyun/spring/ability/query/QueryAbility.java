package net.ximatai.muyun.spring.ability.query;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.Sort;
import net.ximatai.muyun.spring.common.model.contract.EntityContract;

public interface QueryAbility<T extends EntityContract> {
    QueryDescriptor queryDescriptor();

    default QuerySchema querySchema() {
        return QuerySchema.from(queryDescriptor());
    }

    default Criteria queryCriteria(QueryRequest request) {
        return new QueryCompiler(queryDescriptor()).criteria(request);
    }

    default Sort[] querySorts(QueryRequest request) {
        return new QueryCompiler(queryDescriptor()).sorts(request);
    }
}
