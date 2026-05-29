package net.ximatai.muyun.spring.ability;

import net.ximatai.muyun.spring.common.exception.PlatformException;
public class OptimisticLockException extends PlatformException {
    public OptimisticLockException(String message) {
        super(message);
    }
}
