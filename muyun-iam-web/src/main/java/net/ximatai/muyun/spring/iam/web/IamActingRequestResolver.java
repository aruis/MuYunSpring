package net.ximatai.muyun.spring.iam.web;

import jakarta.servlet.http.HttpServletRequest;
import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.common.identity.ActingContext;
import net.ximatai.muyun.spring.common.identity.CurrentUser;
import net.ximatai.muyun.spring.common.platform.ActionExecutionContext;
import net.ximatai.muyun.spring.common.util.Preconditions;
import net.ximatai.muyun.spring.iam.employee.EmployeeDelegationService;
import net.ximatai.muyun.spring.web.ActingRequestResolver;
import org.springframework.stereotype.Component;

import java.util.Optional;

/** IAM 将职员代办请求翻译为平台通用的 {@link ActingContext}。 */
@Component
public class IamActingRequestResolver implements ActingRequestResolver {
    public static final String PRINCIPAL_EMPLOYEE_ID_HEADER = "X-MuYun-Acting-Principal-Employee-Id";
    public static final String PRINCIPAL_POSITION_ID_HEADER = "X-MuYun-Acting-Principal-Position-Id";

    private final EmployeeDelegationService employeeDelegationService;

    public IamActingRequestResolver(EmployeeDelegationService employeeDelegationService) {
        this.employeeDelegationService = employeeDelegationService;
    }

    @Override
    public Optional<ActingContext> resolve(HttpServletRequest request, ActionExecutionContext actionContext) {
        String employeeId = header(request, PRINCIPAL_EMPLOYEE_ID_HEADER);
        String positionId = header(request, PRINCIPAL_POSITION_ID_HEADER);
        if (employeeId == null && positionId == null) return Optional.empty();
        String principalEmployeeId = Preconditions.requireText(employeeId, "principalEmployeeId");
        CurrentUser operator = actionContext.currentUser()
                .orElseThrow(() -> new PlatformException("acting request requires current user"));
        return Optional.of(employeeDelegationService.resolveActingContext(operator, principalEmployeeId, positionId,
                actionContext.moduleAlias(), actionContext.actionCode()));
    }

    private String header(HttpServletRequest request, String name) {
        String value = request.getHeader(name);
        return value == null || value.isBlank() ? null : value.trim();
    }
}
