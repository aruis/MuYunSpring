package net.ximatai.muyun.spring.platform.web;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UiFormulaTest {
    @Test
    void acceptsOnlyPortableBooleanPredicatesThatWebClientsCanEvaluate() {
        assertThat(UiFormula.booleanExpression("PRESENT({directoryId})").expression())
                .isEqualTo("PRESENT({directoryId})");
        assertThat(UiFormula.booleanExpression("!(PRESENT({directoryId}))").expression())
                .isEqualTo("!(PRESENT({directoryId}))");
        assertThat(UiFormula.booleanExpression("PRESENT({directoryId}) && !(PRESENT({id}))").expression())
                .isEqualTo("PRESENT({directoryId}) && !(PRESENT({id}))");
    }

    @Test
    void rejectsServerOnlyFormulaEngineExpressionsFromUiDescriptors() {
        assertThatThrownBy(() -> UiFormula.booleanExpression("ISNULL({directoryId})"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unsupported portable UI Boolean formula");
        assertThatThrownBy(() -> UiFormula.booleanExpression("PRESENT({directory.id})"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unsupported portable UI Boolean formula");
    }

    @Test
    void enabledWhenSerializesThePortableNegationConsumedByTheWebClient() {
        ViewFieldDefinition field = ViewFieldDefinition.field("fileId")
                .enabledWhen(UiFormula.booleanExpression("PRESENT({directoryId})"))
                .build();

        assertThat(field.readOnly().formula().expression()).isEqualTo("!(PRESENT({directoryId}))");
    }

    @Test
    void enabledWhenNegatesPortableConjunctions() {
        ViewFieldDefinition field = ViewFieldDefinition.field("fileId")
                .enabledWhen(UiFormula.booleanExpression("PRESENT({directoryId}) && !(PRESENT({id}))"))
                .build();

        assertThat(field.readOnly().formula().expression())
                .isEqualTo("!(PRESENT({directoryId}) && !(PRESENT({id})))");
    }
}
