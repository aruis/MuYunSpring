package net.ximatai.muyun.spring.platform.code;

public record ResolvedCodeRule(
        CodeRule rule,
        String resolvedOrganizationId
) {
}
