import org.gradle.api.tasks.SourceSetContainer
import org.springframework.boot.gradle.tasks.run.BootRun

plugins {
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
}

configurations.configureEach {
    exclude(group = "org.springframework.boot", module = "spring-boot-starter-jackson")
    exclude(group = "org.springframework.boot", module = "spring-boot-starter-jackson-test")
    exclude(group = "org.springframework.boot", module = "spring-boot-jackson")
}

dependencies {
    implementation(project(":muyun-platform"))
    implementation(project(":muyun-iam"))
    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.boot.starter.websocket)
    implementation(libs.spring.boot.jackson2)
    implementation(libs.spring.boot.starter.jdbc)
    implementation(libs.spring.tx)
    implementation(libs.muyun.database.spring.boot.starter)
    runtimeOnly(libs.postgresql)
    developmentOnly(libs.spring.boot.devtools)

    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.spring.boot.starter.webmvc.test)
    testImplementation(libs.spring.boot.starter.restclient.test)
    testImplementation(libs.testcontainers.junit.jupiter)
    testImplementation(libs.testcontainers.postgresql)
}

val localRuntimeProjectPaths = listOf(
    ":muyun-common",
    ":muyun-ability",
    ":muyun-dynamic",
    ":muyun-platform",
    ":muyun-iam",
)
val localRuntimeOutputs = files(localRuntimeProjectPaths.map { projectPath ->
    rootProject.project(projectPath)
        .extensions
        .getByType<SourceSetContainer>()
        .named("main")
        .map { sourceSet -> sourceSet.output }
})
val localRuntimeBuildDirectories = localRuntimeProjectPaths.map { projectPath ->
    rootProject.project(projectPath).layout.buildDirectory
}

tasks.named<BootRun>("bootRun") {
    // DevTools can only restart against mutable class directories. Do not retain the matching
    // project JARs: duplicate platform classes split parent/child types across DevTools loaders.
    classpath = localRuntimeOutputs + classpath.filter { entry ->
        localRuntimeBuildDirectories.none { buildDirectory ->
            entry.toPath().startsWith(buildDirectory.get().asFile.toPath())
        }
    }
}
