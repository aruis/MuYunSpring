package net.ximatai.muyun.spring.boot.iam;

import net.ximatai.muyun.spring.ability.OptimisticLockException;
import net.ximatai.muyun.spring.common.exception.PlatformException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = {
        TenantWebController.class,
        OrganizationWebController.class,
        RoleWebController.class
})
public class IamWebExceptionHandler {
    @ExceptionHandler({IllegalArgumentException.class, PlatformException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public IamWebError handleBadRequest(RuntimeException exception) {
        return IamWebError.badRequest(exception.getMessage());
    }

    @ExceptionHandler(OptimisticLockException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public IamWebError handleOptimisticLock(OptimisticLockException exception) {
        return IamWebError.conflict(exception.getMessage());
    }
}
