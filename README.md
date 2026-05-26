# MuYunSpring

Java 21 + Spring Boot platform skeleton for a dynamic/static unified platform.

Core ideas:

- Static Java models and dynamic metadata models share the same ability semantics.
- Platform baseline abilities include CRUD, soft delete, lifecycle and cache.
- Declared abilities include tree, sort, reference, child relations and reference dependencies.
- Dynamic runtime is metadata-driven, but still enters the same DAO and ability chain.

Initial Gradle subprojects:

```text
muyun-common
muyun-ability
muyun-module
muyun-iam
muyun-boot
```

Run tests:

```bash
./gradlew test
```

Roadmap:

- [Agent Guide](AGENTS.md)
- [MuYunSpring 里程碑规划](docs/roadmap/MILESTONES.md)
- [开发原则](docs/DEVELOPMENT_PRINCIPLES.md)
- [动静一体核心设计](docs/architecture/DYNAMIC_STATIC_UNIFIED_CORE.md)
- [命名与边界](docs/architecture/NAMING_AND_BOUNDARIES.md)
