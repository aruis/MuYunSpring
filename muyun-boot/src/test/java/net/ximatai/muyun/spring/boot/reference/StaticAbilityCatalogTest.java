package net.ximatai.muyun.spring.boot.reference;

import net.ximatai.muyun.spring.ability.BaseDao;
import net.ximatai.muyun.spring.ability.CrudAbility;
import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.common.model.standard.StandardEntity;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StaticAbilityCatalogTest {
    @Test
    void shouldIndexStaticAbilityByModelAndReferenceTarget() {
        ModelAbility ability = new ModelAbility(FirstRecord.class, "demo.first");

        StaticAbilityCatalog catalog = new StaticAbilityCatalog(List.of(ability));

        assertThat(catalog.findByModel(FirstRecord.class)).containsSame(ability);
        assertThat(catalog.findByTarget(net.ximatai.muyun.spring.ability.reference.ReferenceTarget.of("demo", "first")))
                .containsSame(ability);
    }

    @Test
    void shouldRejectDuplicateStaticReferenceTargets() {
        assertThatThrownBy(() -> new StaticAbilityCatalog(List.of(
                new ModelAbility(FirstRecord.class, "demo.shared"),
                new ModelAbility(SecondRecord.class, "demo.shared"))))
                .isInstanceOf(PlatformException.class)
                .hasMessageContaining("multiple CRUD services are registered for reference target")
                .hasMessageContaining("demo.shared");
    }

    private static final class FirstRecord extends StandardEntity {
    }

    private static final class SecondRecord extends StandardEntity {
    }

    private record ModelAbility(Class<?> modelClass, String moduleAlias) implements CrudAbility<StandardEntity> {
        @Override
        public BaseDao<StandardEntity, String> getDao() {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getModuleAlias() {
            return moduleAlias;
        }
    }
}
