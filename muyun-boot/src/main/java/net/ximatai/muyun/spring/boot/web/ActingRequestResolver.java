package net.ximatai.muyun.spring.boot.web;

import jakarta.servlet.http.HttpServletRequest;
import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.common.identity.ActingContext;
import net.ximatai.muyun.spring.common.identity.CurrentUser;
import net.ximatai.muyun.spring.common.platform.ActionExecutionContext;
import net.ximatai.muyun.spring.common.util.Preconditions;
import net.ximatai.muyun.spring.iam.employee.EmployeeDelegationService;

import java.util.Optional;

public class ActingRequestResolver {
    public static final String PRINCIPAL_EMPLOYEE_ID_HEADER = "X-MuYun-Acting-Principal-Employee-Id";
    public static final String PRINCIPAL_POSITION_ID_HEADER = "X-MuYun-Acting-Principal-Position-Id";

    private final EmployeeDelegationService employeeDelegationService;

    public ActingRequestResolver(EmployeeDelegationService employeeDelegationService) {
        this.employeeDelegationService = employeeDelegationService;
    }

    public Optional<ActingContext> resolve(HttpServletRequest request, ActionExecutionContext actionContext) {
        String principalEmployeeId = header(request, PRINCIPAL_EMPLOYEE_ID_HEADER);
        String principalPositionId = header(request, PRINCIPAL_POSITION_ID_HEADER);
        if (principalEmployeeId == null && principalPositionId == null) {
            return Optional.empty();
        }
        if (employeeDelegationService == null) {
            throw new PlatformException("employee delegation service is not available");
        }
        String validPrincipalEmployeeId = Preconditions.requireText(principalEmployeeId, "principalEmployeeId");
        CurrentUser operator = actionContext.currentUser()
                .orElseThrow(() -> new PlatformException("acting request requires current user"));
        return Optional.of(employeeDelegationService.resolveActingContext(
                operator,
                validPrincipalEmployeeId,
                principalPositionId,
                actionContext.moduleAlias(),
                actionContext.actionCode()
        ));
    }

    public static boolean hasActingRequest(HttpServletRequest request) {
        return headerValue(request, PRINCIPAL_EMPLOYEE_ID_HEADER) != null
                || headerValue(request, PRINCIPAL_POSITION_ID_HEADER) != null;
    }

    private String header(HttpServletRequest request, String name) {
        return headerValue(request, name);
    }

    private static String headerValue(HttpServletRequest request, String name) {
        String value = request.getHeader(name);
        return value == null || value.isBlank() ? null : value.trim();
    }
}
