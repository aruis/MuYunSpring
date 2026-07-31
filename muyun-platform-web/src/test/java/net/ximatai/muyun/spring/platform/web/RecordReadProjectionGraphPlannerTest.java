package net.ximatai.muyun.spring.platform.web;

import net.ximatai.muyun.spring.dynamic.metadata.EntityDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.EntityReferenceDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.FieldDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.ModuleDefinition;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RecordReadProjectionGraphPlannerTest {
    @Test
    void shouldAppendResolvedReferencePathEdgesToProjectionGraph() {
        ModuleDefinition order = ModuleDefinition.builder("crm.order", "订单")
                .entities(List.of(new EntityDefinition(
                        "order",
                        "crm_order",
                        "Order",
                        List.of(
                                FieldDefinition.string("customerId", "客户").column("customer_id"),
                                FieldDefinition.string("orderNo", "订单号").column("order_no")
                        )
                )))
                .relations(List.of())
                .references(List.of(new EntityReferenceDefinition(
                        "order",
                        "customerId",
                        "crm.customer.customer"
                ).withProjection("title", "customerTitle")))
                .build();
        ModuleDefinition customer = new ModuleDefinition(
                "crm.customer",
                "客户",
                List.of(new EntityDefinition(
                        "customer",
                        "crm_customer",
                        "Customer",
                        List.of(FieldDefinition.string("title", "客户名称").column("title"))
                ))
        );
        List<StaticModuleDefinition> definitions = DynamicRelationProjectionDefinitionAdapter.adapt(
                List.of(order, customer));
        StaticModuleDefinition orderDefinition = definitions.stream()
                .filter(definition -> definition.moduleAlias().equals("crm.order"))
                .findFirst()
                .orElseThrow();
        RecordReadProjection projection = new RecordReadProjection(
                "crm.order",
                "dynamic_list",
                List.of(ViewFieldRef.main("orderNo"), ViewFieldRef.main("customerTitle")),
                List.of("id"),
                List.of()
        );

        ProjectionGraph graph = RecordReadProjectionGraphPlanner.plan(definitions, orderDefinition, projection);

        assertThat(graph.nodes()).extracting(ProjectionGraphNode::nodeId)
                .contains("root", "main:id", "main:orderNo", "main:customerTitle", "join:customer_id");
        assertThat(graph.nodes())
                .filteredOn(node -> node.nodeId().equals("join:customer_id"))
                .singleElement()
                .satisfies(node -> {
                    assertThat(node.moduleAlias()).isEqualTo("crm.customer");
                    assertThat(node.entityAlias()).isEqualTo("customer");
                    assertThat(node.tableAlias()).isEqualTo("customer_id");
                });
        assertThat(graph.edges()).filteredOn(edge -> edge.edgeKind() == ProjectionGraphEdgeKind.REFERENCE_JOIN)
                .singleElement()
                .satisfies(edge -> {
                    assertThat(edge.sourceNodeId()).isEqualTo("root");
                    assertThat(edge.targetNodeId()).isEqualTo("join:customer_id");
                    assertThat(edge.tableAlias()).isEqualTo("customer_id");
                    assertThat(edge.cardinality()).isEqualTo(RelationProjectionCardinality.MANY_TO_ONE);
                    assertThat(edge.joinConditions()).hasSize(2);
                });
        assertThat(graph.edges()).filteredOn(edge -> edge.edgeKind() == ProjectionGraphEdgeKind.REFERENCE_OUTPUT_FIELD)
                .singleElement()
                .satisfies(edge -> {
                    assertThat(edge.sourceNodeId()).isEqualTo("join:customer_id");
                    assertThat(edge.targetNodeId()).isEqualTo("main:customerTitle");
                    assertThat(edge.outputFieldName()).isEqualTo("customerTitle");
                    assertThat(edge.targetFieldName()).isEqualTo("title");
                    assertThat(edge.existsProjection()).isFalse();
                    assertThat(edge.path()).isEqualTo("title");
                });
    }
}
