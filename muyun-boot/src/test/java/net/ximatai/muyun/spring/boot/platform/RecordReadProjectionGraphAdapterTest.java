package net.ximatai.muyun.spring.boot.platform;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RecordReadProjectionGraphAdapterTest {
    @Test
    void shouldAdaptRecordReadProjectionToGraphWithoutChangingProjectionFacts() {
        RecordReadProjection projection = new RecordReadProjection(
                "iam.user",
                "list",
                List.of(
                        new ViewFieldRef(null, "username", "username-field"),
                        ViewFieldRef.relation("employee", "employeeNo")
                ),
                List.of("id", "username"),
                List.of(
                        RecordReadPostTransform.fieldProtection("username").serialize(),
                        "unknown:employeeNo"
                )
        );

        ProjectionGraph graph = RecordReadProjectionGraphAdapter.adapt(projection);

        assertThat(graph.moduleAlias()).isEqualTo("iam.user");
        assertThat(graph.viewCode()).isEqualTo("list");
        assertThat(graph.nodes()).extracting(ProjectionGraphNode::nodeId)
                .containsExactly("root", "main:id", "main:username", "relation:employee:employeeNo");
        assertThat(graph.responseFieldNodes()).extracting(ProjectionGraphNode::fieldName)
                .containsExactly("username", "employeeNo");
        assertThat(graph.responseFieldNodes()).extracting(ProjectionGraphNode::fieldId)
                .containsExactly("username-field", null);
        assertThat(graph.internalReadFieldNodes()).extracting(ProjectionGraphNode::fieldName)
                .containsExactly("id", "username");
        assertThat(graph.edges()).extracting(ProjectionGraphEdge::edgeKind)
                .containsExactly(
                        ProjectionGraphEdgeKind.INTERNAL_READ_FIELD,
                        ProjectionGraphEdgeKind.INTERNAL_READ_FIELD,
                        ProjectionGraphEdgeKind.MAIN_OUTPUT_FIELD,
                        ProjectionGraphEdgeKind.RELATION_OUTPUT_FIELD
                );
        assertThat(graph.edges()).extracting(ProjectionGraphEdge::path)
                .containsExactly("id", "username", "username", "employee.employeeNo");
        assertThat(graph.transforms()).extracting(ProjectionGraphTransform::rawExpression)
                .containsExactly("fieldProtection:username", "unknown:employeeNo");
        assertThat(graph.transforms()).extracting(ProjectionGraphTransform::parsed)
                .containsExactly(true, true);
        assertThat(graph.transforms()).extracting(ProjectionGraphTransform::transformType)
                .containsExactly(RecordReadPostTransform.FIELD_PROTECTION, "unknown");
        assertThat(graph.parsedTransforms())
                .containsExactly(
                        RecordReadPostTransform.fieldProtection("username"),
                        new RecordReadPostTransform("unknown", "employeeNo")
                );
        assertThat(RecordReadProjectionPostProcessor.supportsSqlOutput(graph)).isFalse();
    }

    @Test
    void shouldUseGraphTransformsToCheckSupportedSqlOutputPostProcessors() {
        RecordReadProjection projection = new RecordReadProjection(
                "iam.user",
                "list",
                List.of(ViewFieldRef.main("username")),
                List.of("id"),
                List.of(
                        RecordReadPostTransform.fieldProtection("username").serialize(),
                        RecordReadPostTransform.optionTitle("passwordStatus").serialize()
                )
        );

        ProjectionGraph graph = RecordReadProjectionGraphAdapter.adapt(projection);

        assertThat(RecordReadProjectionPostProcessor.supportsSqlOutput(graph)).isTrue();
    }
}
