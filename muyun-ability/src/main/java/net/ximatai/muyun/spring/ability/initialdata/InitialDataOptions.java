package net.ximatai.muyun.spring.ability.initialdata;

public record InitialDataOptions(
        String name,
        InitialDataPhase phase,
        int order,
        InitialDataPolicy policy,
        String tenantId
) {
    public static InitialDataOptions defaults() {
        return new InitialDataOptions(null, InitialDataPhase.SYSTEM_INITIAL_DATA, 100,
                InitialDataPolicy.RECONCILE_MANAGED, null);
    }

    public static InitialDataOptions system(String name, int order) {
        return defaults().name(name).order(order);
    }

    public InitialDataOptions {
        if (phase == null) {
            phase = InitialDataPhase.SYSTEM_INITIAL_DATA;
        }
        if (policy == null) {
            policy = InitialDataPolicy.RECONCILE_MANAGED;
        }
    }

    public InitialDataOptions name(String value) {
        return new InitialDataOptions(value, phase, order, policy, tenantId);
    }

    public InitialDataOptions phase(InitialDataPhase value) {
        return new InitialDataOptions(name, value, order, policy, tenantId);
    }

    public InitialDataOptions order(int value) {
        return new InitialDataOptions(name, phase, value, policy, tenantId);
    }

    public InitialDataOptions policy(InitialDataPolicy value) {
        return new InitialDataOptions(name, phase, order, value, tenantId);
    }

    public InitialDataOptions tenant(String value) {
        return new InitialDataOptions(name, phase, order, policy, value);
    }
}
