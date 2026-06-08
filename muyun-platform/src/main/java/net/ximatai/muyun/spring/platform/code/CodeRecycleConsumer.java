package net.ximatai.muyun.spring.platform.code;

public interface CodeRecycleConsumer {
    CodeRecycleEntry consumeAvailable(String ruleId, String basisKey, String periodKey, String tenantId);
}
