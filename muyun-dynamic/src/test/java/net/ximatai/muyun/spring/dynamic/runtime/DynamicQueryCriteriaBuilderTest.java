package net.ximatai.muyun.spring.dynamic.runtime;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.CriteriaOperator;
import net.ximatai.muyun.spring.dynamic.metadata.DynamicQueryOperator;
import net.ximatai.muyun.spring.dynamic.metadata.EntityDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.FieldDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.FieldType;
import net.ximatai.muyun.spring.dynamic.metadata.ModuleDefinitionException;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
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

    @Test
    void shouldNormalizeDateAndTimestampQueryValues() {
        DynamicQueryCriteriaBuilder builder = new DynamicQueryCriteriaBuilder(timeEntity());

        Criteria criteria = builder.build(List.of(
                DynamicQueryCondition.of("businessDate", DynamicQueryOperator.BETWEEN, "2026-01-01", "2026-01-31"),
                DynamicQueryCondition.of("submittedAt", DynamicQueryOperator.GTE, "2026-01-01T00:00:00Z"),
                DynamicQueryCondition.of("meetingAt", DynamicQueryOperator.LTE, "2026-01-02T00:00:00Z")
        ));

        assertThat(criteria.getClauses().get(0).getValues())
                .containsExactly(LocalDate.parse("2026-01-01"), LocalDate.parse("2026-01-31"));
        assertThat(criteria.getClauses().get(1).getValues())
                .containsExactly(Instant.parse("2026-01-01T00:00:00Z"));
        assertThat(criteria.getClauses().get(2).getValues())
                .containsExactly(Instant.parse("2026-01-02T00:00:00Z"));
    }

    @Test
    void shouldRejectInvalidDateAndTimestampQueryValues() {
        DynamicQueryCriteriaBuilder builder = new DynamicQueryCriteriaBuilder(timeEntity());

        assertThatThrownBy(() -> builder.build(List.of(DynamicQueryCondition.of("businessDate", "2026-13-01"))))
                .isInstanceOf(ModuleDefinitionException.class)
                .hasMessageContaining("invalid query value type");
        assertThatThrownBy(() -> builder.build(List.of(DynamicQueryCondition.of("submittedAt", "2026-01-01"))))
                .isInstanceOf(ModuleDefinitionException.class)
                .hasMessageContaining("invalid query value type");
        assertThatThrownBy(() -> builder.build(List.of(DynamicQueryCondition.of("meetingAt", "2026-01-01T00:00:00+08:00"))))
                .isInstanceOf(ModuleDefinitionException.class)
                .hasMessageContaining("invalid query value type");
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

    private EntityDefinition timeEntity() {
        return new EntityDefinition("contract", "app_contract", "Contract", List.of(
                FieldDefinition.of("businessDate", FieldType.DATE, "Business Date")
                        .queryable(),
                FieldDefinition.timestamp("submittedAt", "Submitted At").queryable(),
                FieldDefinition.zonedTimestamp("meetingAt", "Meeting At").queryable(),
                FieldDefinition.zonedTimestampTimeZone("meetingAt", "meeting_at")
        ));
    }
}
