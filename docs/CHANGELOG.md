# 变更记录

本文件记录面向 MuYunSpring 使用者的正式发布内容：新增能力、行为变化、兼容性影响和迁移要求。

## 0.26.1 - 2026-08-05

### Added

- `muyun-spring-bom`：统一公共平台 artifact 的依赖版本。
- `muyun-spring-boot-starter`：标准 Spring Boot 自动装配入口，业务应用无需依赖 `muyun-boot`。
- Maven Central 发布任务、签名校验、tag gate 和本地消费者仓库验证。

### Changed

- `muyun-boot` 收敛为框架自身的本地运行宿主；平台装配迁移至公共 Starter。
