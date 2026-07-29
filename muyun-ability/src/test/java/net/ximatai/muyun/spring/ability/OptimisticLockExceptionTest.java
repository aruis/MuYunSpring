package net.ximatai.muyun.spring.ability;

import net.ximatai.muyun.spring.common.exception.PlatformErrorCodes;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OptimisticLockExceptionTest {
    @Test
    void shouldDeclareVersionConflictOutsideTheWebLayer() {
        OptimisticLockException exception = new OptimisticLockException("record version conflict");

        assertThat(exception.code()).isEqualTo(PlatformErrorCodes.CONFLICT_VERSION);
        assertThat(exception.httpStatus()).isEqualTo(409);
    }
}
