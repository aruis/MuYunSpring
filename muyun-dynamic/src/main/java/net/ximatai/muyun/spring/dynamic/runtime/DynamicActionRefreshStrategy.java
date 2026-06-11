package net.ximatai.muyun.spring.dynamic.runtime;

public record DynamicActionRefreshStrategy(
        boolean list,
        boolean detail,
        boolean redirectToDetail,
        String redirectRecordId,
        String redirectModuleAlias
) {
    public DynamicActionRefreshStrategy {
        redirectRecordId = normalize(redirectRecordId);
        redirectModuleAlias = normalize(redirectModuleAlias);
        if (redirectToDetail && redirectRecordId == null) {
            throw new IllegalArgumentException("redirectRecordId must not be blank when redirectToDetail is true");
        }
    }

    public static DynamicActionRefreshStrategy none() {
        return new DynamicActionRefreshStrategy(false, false, false, null, null);
    }

    public static DynamicActionRefreshStrategy listRefresh() {
        return new DynamicActionRefreshStrategy(true, false, false, null, null);
    }

    public static DynamicActionRefreshStrategy detailRefresh() {
        return new DynamicActionRefreshStrategy(false, true, false, null, null);
    }

    public static DynamicActionRefreshStrategy listAndDetail() {
        return new DynamicActionRefreshStrategy(true, true, false, null, null);
    }

    public static DynamicActionRefreshStrategy redirectToDetail(String recordId) {
        return redirectToDetail(recordId, null);
    }

    public static DynamicActionRefreshStrategy redirectToDetail(String recordId, String moduleAlias) {
        return new DynamicActionRefreshStrategy(false, true, true, recordId, moduleAlias);
    }

    public boolean active() {
        return list || detail || redirectToDetail;
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
