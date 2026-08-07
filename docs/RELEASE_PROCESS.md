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
再依次执行发布 gate、工作区清理、后端与消费者验证，以及远端发布任务。清理必须发生在验证之前：npm 消费者
验证会生成正式发布使用的 staging 包，之后不得再次清理该目录。

需要配置以下 GitHub Actions secrets：

- `SONATYPE_TOKEN`、`SONATYPE_PASSWORD`
- `SIGNING_KEY_ID`
- `SIGNING_SECRET_KEY` 或 `SIGNING_SECRET_KEY_BASE64`
- `SIGNING_PASSWORD`
- `NPM_TOKEN`：`@ximatai/muyun-web-app` 的 npm 官方仓库发布 token。使用具备包读写权限、覆盖
  `@ximatai` scope 且启用 `bypass 2FA` 的 granular token；否则 npm 会在实际 publish 阶段拒绝发布。

本地预检：

```bash
./gradlew verifyReleaseTagVersion verifyReleaseCredentials -Prelease.tag=v<version>
```

每次正式发布按以下顺序进行：

1. 在 `main` 更新 [变更记录](CHANGELOG.md)；`muyunVersion` 保持待发布版本的 `-SNAPSHOT`。
2. 推送匹配该版本的 tag，例如 `muyunVersion=0.26.2-SNAPSHOT` 时推送 `git tag v0.26.2 && git push origin v0.26.2`。
3. 发布成功后，Release workflow 不修改 `main`；开始下一轮开发时，在正常业务 PR 中将 `muyunVersion` 推进到下一个目标版本，例如 `0.26.3-SNAPSHOT`。

tag 必须与当前 `muyunVersion` 去掉 `-SNAPSHOT` 后完全一致。发布任务自身依赖 tag/version 与凭证 gate；Release workflow
按以下顺序执行：

1. 校验 tag、版本和发布凭据。
2. 清理工作区。
3. 执行 `verifyAll`、本地 Maven 消费者验证、npm 消费者验证和 npm publish dry-run。
4. 执行 `./gradlew publishReleaseToSonatype`。
5. 从 npm 消费者验证生成的 staging 包发布同一 tag 对应的 npm 包。

Maven Central 与 npm 都发布成功才代表一次完整发布。Maven Central 的索引可见性检查保留为发布后的轻量人工验证，
不阻塞 Release workflow。

前端 npm 包使用同一个正式版本：`muyun-web/package.json` 的 `version` 必须等于 `muyunVersion` 去掉
`-SNAPSHOT` 的结果，`pack:consumer` 会在构建前强制校验。这样 Maven tag、npm tarball 与源代码版本保持可追溯一致。

## 单通道发布补偿

若 Release 已成功发布 Maven Central、但 npm 因临时凭据或外部故障漏发，不要重跑完整 tag workflow：Maven Central
构件不可用同一版本重复上传。仅从该 release tag 建立干净 worktree，重新生成并校验 npm 包后补发缺失通道：

```bash
npm ci --prefix muyun-web
npm run pack:consumer --prefix muyun-web
cd build/consumer-npm/staging/web-app
npm publish --dry-run --access public --registry=https://registry.npmjs.org/
npm publish --access public --registry=https://registry.npmjs.org/
```

补偿只允许发布 registry 中尚不存在的同版本包，并应在发布后回读该版本与 `latest` tag。该路径是异常恢复，不替代
GitHub Actions 的常规发布；恢复完成后应修正对应的 CI 或凭据配置，避免下一次 tag 重复进入补偿流程。

## pre-FieldSpec schema 升级

`FieldCatalogLegacySchemaBridge` 已移除，不再由 `local` profile 自动修改数据库。升级保留该阶段数据库的环境，应先停止应用并备份，再执行：

```bash
psql "$DATABASE_URL" -v ON_ERROR_STOP=1 \
  -f scripts/migrations/field-catalog-pre-fieldspec-postgresql.sql
```

脚本只支持 PostgreSQL 的 `public` schema，包含旧字段目录表/列重命名、必填字段回填和旧
`platform_metadata_field.field_type_alias` 的数据合并。执行成功后再部署当前版本；不要在应用运行期间执行。
