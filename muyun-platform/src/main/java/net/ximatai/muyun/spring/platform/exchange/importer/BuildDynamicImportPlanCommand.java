package net.ximatai.muyun.spring.platform.exchange.importer;

import net.ximatai.muyun.spring.common.exception.PlatformException;

import java.util.List;

public record BuildDynamicImportPlanCommand(
        String moduleAlias,
        String mainMatchFieldName,
        ImportDuplicateStrategy mainDuplicateStrategy,
        List<ChildSheetCommand> childSheets
) {
    public BuildDynamicImportPlanCommand {
        requireText(moduleAlias, "moduleAlias must not be blank");
        requireText(mainMatchFieldName, "mainMatchFieldName must not be blank");
        if (mainDuplicateStrategy == null) {
            throw new PlatformException("mainDuplicateStrategy must not be null");
        }
        childSheets = childSheets == null ? List.of() : List.copyOf(childSheets);
    }

    public record ChildSheetCommand(
            String entityAlias,
            String matchFieldName,
            ImportDuplicateStrategy duplicateStrategy
    ) {
        public ChildSheetCommand {
            requireText(entityAlias, "child entityAlias must not be blank");
            requireText(matchFieldName, "child matchFieldName must not be blank");
            if (duplicateStrategy == null) {
                throw new PlatformException("child duplicateStrategy must not be null: " + entityAlias);
            }
        }
    }

    private static void requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new PlatformException(message);
        }
    }
}
