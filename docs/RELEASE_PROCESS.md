# 发布流程

MuYunSpring 将平台基础能力、Web 交付和 Spring Boot 自动装配发布为 Maven artifact。业务应用通过
`muyun-spring-bom` 统一版本，并依赖 `muyun-spring-boot-starter` 启动平台；`muyun-demo*` 与
`muyun-boot` 不属于公共运行时发布面。

## 本地消费者验证

在框架仓库执行：

```bash
./gradlew verifyAll verifyPublishedConsumer
```

全部公共 artifact 会写入根目录 `build/consumer-repo`。`verifyPublishedConsumer` 随后构建并启动
`samples/published-consumer`；该工程只以 Maven 坐标解析 BOM 与 Starter，并使用独立 PostgreSQL。
这验证 POM 的传递依赖和 Spring Boot 自动装配，而不依赖 Gradle project dependency。

首次正式发布或发布链路发生调整后，可人工运行 `verifyMavenCentralConsumer`。它会等待 BOM 出现在 Maven
Central，再以远端仓库运行同一个消费者，用于确认公开仓库解析与运行。该检查不进入 Release workflow，避免
Maven Central 索引延迟放大为发布流水线风险。

发布完成后 `main` 通常已进入下一个 `-SNAPSHOT` 版本，人工验证时应显式传入刚发布的版本：

```bash
MUYUN_RELEASE_VERSION=<released-version> ./gradlew verifyMavenCentralConsumer
```

## Maven Central 发布

`gradle.properties` 的 `muyunVersion` 只表示下一开发版本，必须保持 `-SNAPSHOT`。发布使用与其去掉
`-SNAPSHOT` 后一致的 `v<version>` tag 触发 `.github/workflows/release.yml`；workflow 从 tag 推导正式构件版本，
再依次执行 `verifyAll`、本地消费者预检、独立的发布工作区清理和远端发布任务。消费者预检与发布工作区清理
必须分开执行：前者需要本地 Maven 仓库，后者则保证上传目录只包含本次构建的构件。

需要配置以下 GitHub Actions secrets：

- `SONATYPE_TOKEN`、`SONATYPE_PASSWORD`
- `SIGNING_KEY_ID`
- `SIGNING_SECRET_KEY` 或 `SIGNING_SECRET_KEY_BASE64`
- `SIGNING_PASSWORD`
- `NPM_TOKEN`：`@ximatai/muyun-web-app` 的 npm 发布 token。

本地预检：

```bash
./gradlew verifyReleaseTagVersion verifyReleaseCredentials -Prelease.tag=v<version>
```

每次正式发布按以下顺序进行：

1. 在 `main` 更新 [变更记录](CHANGELOG.md)；`muyunVersion` 保持待发布版本的 `-SNAPSHOT`。
2. 推送匹配该版本的 tag，例如 `muyunVersion=0.26.2-SNAPSHOT` 时推送 `git tag v0.26.2 && git push origin v0.26.2`。
3. 发布成功后，Release workflow 不修改 `main`；开始下一轮开发时，在正常业务 PR 中将 `muyunVersion` 推进到下一个目标版本，例如 `0.26.3-SNAPSHOT`。

tag 必须与当前 `muyunVersion` 去掉 `-SNAPSHOT` 后完全一致。发布任务自身依赖 tag/version 与凭证 gate；Release workflow
负责在发布前完成消费者验证和独立清理。发布任务为 `./gradlew publishReleaseToSonatype`；同一 tag 的 npm 包在 Maven
上传任务成功后发布。

前端 npm 包使用同一个正式版本：`muyun-web/package.json` 的 `version` 必须等于 `muyunVersion` 去掉
`-SNAPSHOT` 的结果，`pack:consumer` 会在构建前强制校验。这样 Maven tag、npm tarball 与源代码版本保持可追溯一致。

## pre-FieldSpec schema 升级

`FieldCatalogLegacySchemaBridge` 已移除，不再由 `local` profile 自动修改数据库。升级保留该阶段数据库的环境，应先停止应用并备份，再执行：

```bash
psql "$DATABASE_URL" -v ON_ERROR_STOP=1 \
  -f scripts/migrations/field-catalog-pre-fieldspec-postgresql.sql
```

脚本只支持 PostgreSQL 的 `public` schema，包含旧字段目录表/列重命名、必填字段回填和旧
`platform_metadata_field.field_type_alias` 的数据合并。执行成功后再部署当前版本；不要在应用运行期间执行。
