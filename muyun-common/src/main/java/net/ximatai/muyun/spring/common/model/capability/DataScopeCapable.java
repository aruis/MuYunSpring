package net.ximatai.muyun.spring.common.model.capability;

public interface DataScopeCapable {
    String getAuthUserId();

    void setAuthUserId(String authUserId);

    String getAuthAssigneeIds();

    void setAuthAssigneeIds(String authAssigneeIds);

    String getAuthMemberIds();

    void setAuthMemberIds(String authMemberIds);

    String getAuthOrganizationId();

    void setAuthOrganizationId(String authOrganizationId);

    String getAuthModuleAlias();

    void setAuthModuleAlias(String authModuleAlias);
}
