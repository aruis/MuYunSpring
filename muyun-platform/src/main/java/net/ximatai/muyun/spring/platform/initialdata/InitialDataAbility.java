package net.ximatai.muyun.spring.platform.initialdata;

import net.ximatai.muyun.spring.common.identity.CurrentUser;
import net.ximatai.muyun.spring.common.identity.CurrentUserContext;
import net.ximatai.muyun.spring.common.tenant.TenantContext;

import java.util.Comparator;
import java.util.List;

public class InitialDataAbility {
    public static final String SYSTEM_OPERATOR_ID = "platform-initial-data";

    private final List<InitialDataContribution> contributions;

    public InitialDataAbility(List<InitialDataContribution> contributions) {
        this.contributions = contributions == null ? List.of() : List.copyOf(contributions);
    }

    public InitialDataExecutionReport initializeAll() {
        try (TenantContext.Scope ignored = TenantContext.system("initialize platform data");
             CurrentUserContext.Scope ignoredUser = CurrentUserContext.use(
                     CurrentUser.systemUser(SYSTEM_OPERATOR_ID, "Platform Initial Data"))) {
            List<InitialDataContributionReport> reports = contributions.stream()
                    .sorted(Comparator.comparingInt(InitialDataContribution::order)
                            .thenComparing(InitialDataContribution::name))
                    .map(this::execute)
                    .toList();
            return new InitialDataExecutionReport(reports);
        }
    }

    private InitialDataContributionReport execute(InitialDataContribution contribution) {
        InitialDataContext context = new InitialDataContext();
        contribution.contribute(context);
        return new InitialDataContributionReport(contribution.name(), contribution.order(), context.results());
    }
}
