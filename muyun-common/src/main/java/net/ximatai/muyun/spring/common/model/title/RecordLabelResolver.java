package net.ximatai.muyun.spring.common.model.title;

import net.ximatai.muyun.spring.common.model.capability.TitledCapable;
import net.ximatai.muyun.spring.common.model.contract.EntityContract;

import java.util.Optional;

/**
 * Resolves the user-facing label of one platform record from model facts.
 *
 * <p>The title-field declaration is the single source of truth. The titled
 * capability and record id remain compatibility fallbacks for models which
 * have not declared a title field yet.</p>
 */
public final class RecordLabelResolver {
    private RecordLabelResolver() {
    }

    public static Optional<String> resolveFieldName(Class<?> modelClass) {
        Optional<String> declared = TitleFieldResolver.resolveFieldName(modelClass);
        if (declared.isPresent()) {
            return declared;
        }
        return modelClass != null && TitledCapable.class.isAssignableFrom(modelClass)
                ? Optional.of("title")
                : Optional.empty();
    }

    public static String readAsString(EntityContract record) {
        if (record == null) {
            return null;
        }
        String declaredTitle = normalize(TitleFieldResolver.readAsString(record));
        if (declaredTitle != null) {
            return declaredTitle;
        }
        if (record instanceof TitledCapable titled) {
            String compatibleTitle = normalize(titled.getTitle());
            if (compatibleTitle != null) {
                return compatibleTitle;
            }
        }
        return normalize(record.getId());
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
