package net.ximatai.muyun.spring.migration;

import java.util.List;

/**
 * Subclass this as a Spring {@code @Component} to declare a versioned data migration.
 *
 * <pre>{@code
 * @Component
 * class FooMigrate extends AbstractMigration {
 *     private final FooService fooService;
 *
 *     FooMigrate(FooService fooService) {
 *         this.fooService = fooService;
 *     }
 *
 *     \@Override
 *     public String getAlias() {
 *         return "foo";
 *     }
 *
 *     \@Override
 *     public List<MigrateStep> getMigrateSteps() {
 *         return List.of(
 *             new MigrateStep(1, () -> fooService.bumpAllAgesBy(10)),
 *             new MigrateStep(2, () -> fooService.createSeed("seed", 30))
 *         );
 *     }
 * }
 * }</pre>
 *
 * <p>Steps run once on application startup, in version order, skipping any already applied. Each
 * step is atomic: its data changes and the version bump commit together, so a failed step never
 * leaves a partially-applied version behind.
 */
public abstract class AbstractMigration {

    /** Stable, unique migration identifier. */
    public abstract String getAlias();

    /** Ordered migration steps. Versions must be positive and unique. */
    public abstract List<MigrateStep> getMigrateSteps();
}
