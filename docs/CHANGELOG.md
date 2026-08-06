# 变更记录

本文件记录面向 MuYunSpring 使用者的正式发布内容：新增能力、行为变化、兼容性影响和迁移要求。

## Unreleased

### Changed

- 发布前以本地 Maven 坐标启动独立消费者；Maven Central 真实坐标验证保留为首发或发布链路调整后的人工检查。

### Removed

- 不再在 `local` profile 启动时自动升级 pre-FieldSpec 字段目录 schema；升级前须在应用停机后执行
  `scripts/migrations/field-catalog-pre-fieldspec-postgresql.sql`。

## 0.26.1 - 2026-08-05

### Added

- `muyun-spring-bom`：统一公共平台 artifact 的依赖版本。
- `muyun-spring-boot-starter`：标准 Spring Boot 自动装配入口，业务应用无需依赖 `muyun-boot`。
- Maven Central 发布任务、签名校验、tag gate 和本地消费者仓库验证。

### Changed

- `muyun-boot` 收敛为框架自身的本地运行宿主；平台装配迁移至公共 Starter。
