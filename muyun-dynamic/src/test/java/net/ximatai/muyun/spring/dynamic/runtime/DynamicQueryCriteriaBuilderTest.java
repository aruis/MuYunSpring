package net.ximatai.muyun.spring.dynamic.runtime;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.CriteriaOperator;
import net.ximatai.muyun.spring.dynamic.metadata.DynamicQueryOperator;
import net.ximatai.muyun.spring.dynamic.metadata.EntityDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.FieldDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.ModuleDefinitionException;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Collections;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DynamicQueryCriteriaBuilderTest {
    @Test
    void shouldBuildCriteriaOnlyForConfiguredQueryableFields() {
        DynamicQueryCriteriaBuilder builder = new DynamicQueryCriteriaBuilder(entity());

        Criteria criteria = builder.build(List.of(
                DynamicQueryCondition.of("title", "Acme"),
                DynamicQueryCondition.of("amount", DynamicQueryOperator.BETWEEN, 10, 20),
                DynamicQueryCondition.of("status", DynamicQueryOperator.IN, "active", "frozen")
        ));

        assertThat(criteria.getClauses()).hasSize(3);
        assertThat(criteria.getClauses().get(0).getField()).isEqualTo("title");
        assertThat(criteria.getClauses().get(0).getOperator()).isEqualTo(CriteriaOperator.LIKE);
        assertThat(criteria.getClauses().get(1).getOperator()).isEqualTo(CriteriaOperator.BETWEEN);
        assertThat(criteria.getClauses().get(2).getOperator()).isEqualTo(CriteriaOperator.IN);
    }

    @Test
    void shouldRejectUnknownUnqueryableAndDisallowedOperator() {
        DynamicQueryCriteriaBuilder builder = new DynamicQueryCriteriaBuilder(entity());

        assertThatThrownBy(() -> builder.build(List.of(DynamicQueryCondition.of("missing", "x"))))
                .isInstanceOf(ModuleDefinitionException.class)
                .hasMessageContaining("unknown query field");
        assertThatThrownBy(() -> builder.build(List.of(DynamicQueryCondition.of("remark", "x"))))
                .isInstanceOf(ModuleDefinitionException.class)
                .hasMessageContaining("not queryable");
        assertThatThrownBy(() -> builder.build(List.of(DynamicQueryCondition.of("status", DynamicQueryOperator.LIKE, "active"))))
                .isInstanceOf(ModuleDefinitionException.class)
                .hasMessageContaining("not allowed");
        assertThatThrownBy(() -> builder.build(List.of(DynamicQueryCondition.of("title", DynamicQueryOperator.LIKE, "a", "b"))))
                .isInstanceOf(ModuleDefinitionException.class)
                .hasMessageContaining("exactly one value");
        assertThatThrownBy(() -> builder.build(List.of(DynamicQueryCondition.of("amount", DynamicQueryOperator.BETWEEN, 10, 20, 30))))
                .isInstanceOf(ModuleDefinitionException.class)
                .hasMessageContaining("exactly two values");
        assertThatThrownBy(() -> builder.build(List.of(new DynamicQueryCondition(
                "title", DynamicQueryOperator.LIKE, Collections.singletonList(null)))))
                .isInstanceOf(ModuleDefinitionException.class)
                .hasMessageContaining("null query value");
    }

    private EntityDefinition entity() {
        return new EntityDefinition("customer", "crm_customer", "Customer", List.of(
                FieldDefinition.titleField().queryable(),
                FieldDefinition.decimal("amount", "Amount").queryable(),
                FieldDefinition.string("status", "Status")
                        .queryable(DynamicQueryOperator.EQ, Set.of(DynamicQueryOperator.EQ, DynamicQueryOperator.IN)),
                FieldDefinition.text("remark", "Remark")
        ));
    }
}
