package net.ximatai.muyun.spring.platform.initialdata;

import net.ximatai.muyun.spring.common.exception.PlatformException;

public class InitialDataConflictException extends PlatformException {
    public InitialDataConflictException(String message) {
        super(message);
    }
}
