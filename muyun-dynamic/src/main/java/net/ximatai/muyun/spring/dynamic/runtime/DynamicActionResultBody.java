package net.ximatai.muyun.spring.dynamic.runtime;

import net.ximatai.muyun.database.core.orm.PageResult;

import java.util.Collection;
import java.util.Map;

public record DynamicActionResultBody(
        DynamicActionResultType type,
        Object value,
        String message,
        boolean refresh,
        String redirectTo,
        DynamicActionRefreshStrategy refreshStrategy
) {
    public DynamicActionResultBody {
        type = type == null ? inferType(value) : type;
        message = message == null || message.isBlank() ? null : message.trim();
        redirectTo = redirectTo == null || redirectTo.isBlank() ? null : redirectTo.trim();
        refreshStrategy = refreshStrategy == null ? DynamicActionRefreshStrategy.none() : refreshStrategy;
        refresh = refresh || refreshStrategy.active();
    }

    public static DynamicActionResultBody of(Object value) {
        return new DynamicActionResultBody(null, value, null, false, null, null);
    }

    public static DynamicActionResultBody createdRecordId(String value) {
        return new DynamicActionResultBody(DynamicActionResultType.RECORD_ID, value, null, true, null,
                DynamicActionRefreshStrategy.detailRefresh());
    }

    public static DynamicActionResultBody changedCount(int value) {
        return new DynamicActionResultBody(DynamicActionResultType.COUNT, value, null, value > 0, null,
                value > 0 ? DynamicActionRefreshStrategy.listAndDetail() : null);
    }

    public static DynamicActionResultBody changedCount(int value, String message) {
        return changedCount(value).message(message);
    }

    public static DynamicActionResultBody none() {
        return new DynamicActionResultBody(DynamicActionResultType.NONE, null, null, false, null, null);
    }

    public static DynamicActionResultBody notice(String message) {
        return none().message(message);
    }

    public static DynamicActionResultBody refreshed() {
        return none().withRefresh();
    }

    public static DynamicActionResultBody refreshed(Object value) {
        return of(value).withRefresh();
    }

    public static DynamicActionResultBody refreshedNotice(String message) {
        return refreshed().message(message);
    }

    public static DynamicActionResultBody redirect(String value) {
        return none().redirectTo(value);
    }

    public static DynamicActionResultBody redirect(String value, String message) {
        return redirect(value).message(message);
    }

    public static DynamicActionResultBody dialog(String dialogKey, String title) {
        return dialog(new DynamicActionDialog(dialogKey, title));
    }

    public static DynamicActionResultBody dialog(DynamicActionDialog dialog) {
        return new DynamicActionResultBody(DynamicActionResultType.DIALOG, dialog, null, false, null, null);
    }

    public DynamicActionResultBody message(String value) {
        return new DynamicActionResultBody(type, this.value, value, refresh, redirectTo, refreshStrategy);
    }

    public DynamicActionResultBody withRefresh() {
        return withRefreshStrategy(DynamicActionRefreshStrategy.listAndDetail());
    }

    public DynamicActionResultBody withRefreshStrategy(DynamicActionRefreshStrategy strategy) {
        return new DynamicActionResultBody(type, value, message, refresh, redirectTo, strategy);
    }

    public DynamicActionResultBody redirectTo(String value) {
        return new DynamicActionResultBody(type, this.value, message, refresh, value, refreshStrategy);
    }

    private static DynamicActionResultType inferType(Object value) {
        if (value == null) {
            return DynamicActionResultType.NONE;
        }
        if (value instanceof DynamicRecord) {
            return DynamicActionResultType.RECORD;
        }
        if (value instanceof PageResult<?>) {
            return DynamicActionResultType.PAGE;
        }
        if (value instanceof Collection<?>) {
            return DynamicActionResultType.LIST;
        }
        if (value instanceof String || value instanceof Number || value instanceof Boolean || value instanceof Enum<?>) {
            return DynamicActionResultType.VALUE;
        }
        if (value instanceof DynamicActionDialog) {
            return DynamicActionResultType.DIALOG;
        }
        if (value instanceof Map<?, ?>) {
            return DynamicActionResultType.OBJECT;
        }
        return DynamicActionResultType.OBJECT;
    }
}
