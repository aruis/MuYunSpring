package net.ximatai.muyun.spring.platform.code;

import java.util.Map;

@FunctionalInterface
public interface CodeValueUniquenessChecker {
    boolean exists(ResolvedCodeRule resolvedRule, String generatedValue, Map<String, Object> context);
}
