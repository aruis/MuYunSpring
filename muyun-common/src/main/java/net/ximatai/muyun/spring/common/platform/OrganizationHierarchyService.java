package net.ximatai.muyun.spring.common.platform;

import java.util.List;

public interface OrganizationHierarchyService {

    /**
     * Returns organization ids from the current organization to the root organization.
     * The result must include {@code organizationId} as the first item and must not be null.
     */
    List<String> organizationIdsFromSelfToRoot(String organizationId);
}
