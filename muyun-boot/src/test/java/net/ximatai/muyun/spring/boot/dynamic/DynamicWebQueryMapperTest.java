package net.ximatai.muyun.spring.boot.dynamic;

import net.ximatai.muyun.database.core.orm.SortDirection;
import net.ximatai.muyun.spring.boot.web.WebPageRequest;
import net.ximatai.muyun.spring.boot.web.WebQueryCondition;
import net.ximatai.muyun.spring.boot.web.WebSort;
import net.ximatai.muyun.spring.dynamic.metadata.DynamicQueryOperator;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DynamicWebQueryMapperTest {
    @Test
    void shouldMapCommonWebQueryConditionAndKeepDefaultOperatorWhenOmitted() {
        var conditions = DynamicWebQueryMapper.queryConditions(List.of(
                new WebQueryCondition("code", null, List.of("C-001")),
                new WebQueryCondition("amount", "GT", List.of(10))
        ));

        assertThat(conditions).hasSize(2);
        assertThat(conditions.get(0).fieldName()).isEqualTo("code");
        assertThat(conditions.get(0).operator()).isNull();
        assertThat(conditions.get(0).values()).isEqualTo(List.of("C-001"));
        assertThat(conditions.get(1).operator()).isEqualTo(DynamicQueryOperator.GT);
    }

    @Test
    void shouldNormalizeDynamicPageAndSorts() {
        var page = DynamicWebQueryMapper.page(new WebPageRequest(0, 999));
        var sorts = DynamicWebQueryMapper.sorts(List.of(
                new WebSort("amount", true),
                new WebSort("code", false)
        ));

        assertThat(page.getOffset()).isZero();
        assertThat(page.getLimit()).isEqualTo(500);
        assertThat(sorts[0].getField()).isEqualTo("amount");
        assertThat(sorts[0].getDirection()).isEqualTo(SortDirection.DESC);
        assertThat(sorts[1].getDirection()).isEqualTo(SortDirection.ASC);
    }
}
