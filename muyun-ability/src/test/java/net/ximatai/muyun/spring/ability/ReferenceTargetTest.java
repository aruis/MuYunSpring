package net.ximatai.muyun.spring.ability;

import net.ximatai.muyun.spring.ability.reference.ReferenceTarget;


import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReferenceTargetTest {
    @Test
    void shouldKeepMultiSegmentModuleAliasWhenParsingQualifiedName() {
        ReferenceTarget target = ReferenceTarget.parse("sales.invoice.invoice");

        assertThat(target.moduleAlias()).isEqualTo("sales.invoice");
        assertThat(target.entityCode()).isEqualTo("invoice");
        assertThat(target.qualifiedName()).isEqualTo("sales.invoice.invoice");
    }

    @Test
    void shouldRejectBlankOrIncompleteQualifiedName() {
        assertThatThrownBy(() -> ReferenceTarget.parse("sales"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("reference target");
        assertThatThrownBy(() -> ReferenceTarget.of("sales.invoice", " "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("entityCode");
    }
}
