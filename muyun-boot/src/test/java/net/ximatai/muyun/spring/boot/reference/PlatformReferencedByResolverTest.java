package net.ximatai.muyun.spring.boot.reference;

import net.ximatai.muyun.spring.ability.BaseDao;
import net.ximatai.muyun.spring.ability.CrudAbility;
import net.ximatai.muyun.spring.ability.reference.ReferenceTo;
import net.ximatai.muyun.spring.ability.reference.ReferencedBy;
import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.common.model.standard.StandardEntity;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PlatformReferencedByResolverTest {
    @Test
    void shouldValidateReferencedBySourceServiceAtStartup() {
        assertThatCode(() -> new PlatformReferencedByResolver(new StaticAbilityCatalog(List.of(
                new ModelAbility(TargetRecord.class), new ModelAbility(SourceRecord.class)))))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldRejectMissingReferencedBySourceServiceAtStartup() {
        assertThatThrownBy(() -> new PlatformReferencedByResolver(new StaticAbilityCatalog(List.of(new ModelAbility(TargetRecord.class)))))
                .isInstanceOf(PlatformException.class)
                .hasMessageContaining("@ReferencedBy source service is not registered")
                .hasMessageContaining(SourceRecord.class.getName());
    }

    private static final class TargetRecord extends StandardEntity {
        @ReferencedBy
        private transient List<SourceRecord> sources;
    }

    private static final class SourceRecord extends StandardEntity {
        @ReferenceTo(target = TargetRecordService.class)
        private String targetRecordId;
    }

    public static final class TargetRecordService {
        public static final String MODULE_ALIAS = "test.targetRecord";
    }

    private record ModelAbility(Class<?> modelClass) implements CrudAbility<StandardEntity> {
        @Override
        public BaseDao<StandardEntity, String> getDao() {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getModuleAlias() {
            return "test." + modelClass.getSimpleName();
        }
    }
}
