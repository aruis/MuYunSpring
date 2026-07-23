package net.ximatai.muyun.spring.common.exception;

/** Raised when a tenant user invokes a module whose application is not opened for that tenant. */
public class ApplicationNotOpenedException extends PlatformException {
    public ApplicationNotOpenedException(String tenantId, String applicationAlias) {
        super(PlatformErrorCodes.APPLICATION_NOT_OPENED, 403,
                "application is not opened for tenant: " + applicationAlias + "." + tenantId);
    }
}
