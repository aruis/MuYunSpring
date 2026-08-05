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

## Maven Central 发布

发布使用 `v<version>` Git tag 触发 `.github/workflows/release.yml`。版本号必须先写入根目录
`build.gradle.kts` 并合并到 `main`；workflow 只验证 tag 与该版本一致，再执行 `verifyAll`、消费者验证和发布任务。

需要配置以下 GitHub Actions secrets：

- `SONATYPE_TOKEN`、`SONATYPE_PASSWORD`
- `SIGNING_KEY_ID`
- `SIGNING_SECRET_KEY` 或 `SIGNING_SECRET_KEY_BASE64`
- `SIGNING_PASSWORD`

本地预检：

```bash
./gradlew verifyReleaseTagVersion verifyReleaseCredentials -Prelease.tag=v0.26.1
```

每次正式发布按以下顺序进行：

1. 将 `build.gradle.kts` 的版本从 `-SNAPSHOT` 更新为正式版本，并更新 [变更记录](CHANGELOG.md)。
2. 合并版本与变更记录到 `main`。
3. 推送匹配的 tag，例如 `git tag v0.26.1 && git push origin v0.26.1`。

只有非 `-SNAPSHOT` 版本且 tag 与版本完全一致时，发布流程才会继续。发布任务自身也依赖
tag/version gate 与消费者验证，不能通过本地命令绕过。发布任务为 `./gradlew publishReleaseToSonatype`。
