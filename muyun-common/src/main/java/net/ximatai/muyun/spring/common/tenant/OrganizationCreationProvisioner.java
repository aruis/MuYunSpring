package net.ximatai.muyun.spring.common.tenant;

public interface OrganizationCreationProvisioner {
    void afterOrganizationCreated(String tenantId, String organizationId);
}
