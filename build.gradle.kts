plugins {
    alias(libs.plugins.spring.boot) apply false
    alias(libs.plugins.spring.dependency.management) apply false
    java
}

allprojects {
    group = "net.ximatai.muyun.spring"
    version = "0.1.0-SNAPSHOT"
}

val testcontainersVersion = libs.versions.testcontainers.get()

subprojects {
    apply(plugin = "java-library")

    extra["testcontainers.version"] = testcontainersVersion

    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(21))
        }
    }

    tasks.withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
        options.compilerArgs.add("-parameters")
    }

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
        maxParallelForks = 1
        forkEvery = 0
        systemProperty("junit.jupiter.execution.parallel.enabled", "false")

        if (name == "test") {
            exclude("**/*IT.class")
        }

        if (project.name == "muyun-platform") {
            reports.html.required.set(false)
            reports.junitXml.includeSystemOutLog.set(false)
            reports.junitXml.includeSystemErrLog.set(false)
        }
    }

    val testSourceSet = extensions.getByType<SourceSetContainer>().named("test")
    tasks.register<Test>("integrationTest") {
        description = "Runs integration tests against real external resources such as Testcontainers."
        group = LifecycleBasePlugin.VERIFICATION_GROUP
        testClassesDirs = testSourceSet.get().output.classesDirs
        classpath = testSourceSet.get().runtimeClasspath
        shouldRunAfter(tasks.named("test"))
        include("**/*IT.class")
    }

    dependencies {
        "compileOnly"(rootProject.libs.lombok)
        "annotationProcessor"(rootProject.libs.lombok)
        "testCompileOnly"(rootProject.libs.lombok)
        "testAnnotationProcessor"(rootProject.libs.lombok)
    }
}

val unitTestTasks = subprojects.map { it.tasks.named<Test>("test") }
val integrationTestTasks = subprojects.map { it.tasks.named<Test>("integrationTest") }

tasks.register("demoClasses") {
    description = "Compiles the standard application and optional demo delivery for local demo development."
    group = LifecycleBasePlugin.BUILD_GROUP
    dependsOn(":muyun-boot:classes", ":muyun-demo-web:classes")
}

tasks.register("demoBootRun") {
    description = "Runs the standard application with the optional school demo on its runtime classpath."
    group = "application"
    dependsOn(":muyun-boot:demoBootRun")
}

val coreModulePaths = setOf(
    ":muyun-common",
    ":muyun-ability",
    ":muyun-dynamic",
    ":muyun-platform",
    ":muyun-iam",
    ":muyun-demo",
)
val deliveryModulePaths = setOf(
    ":muyun-web-adapter",
    ":muyun-platform-web",
    ":muyun-iam-web",
    ":muyun-dynamic-web",
    ":muyun-demo-web",
)
val webDeliveryModulePaths = deliveryModulePaths - ":muyun-web-adapter"
val productionDependencyConfigurations = setOf(
    "api",
    "implementation",
    "compileOnly",
    "compileOnlyApi",
    "runtimeOnly",
)
val allowedProductionProjectDependencies = mapOf(
    ":muyun-common" to emptySet(),
    ":muyun-ability" to setOf(":muyun-common"),
    ":muyun-dynamic" to setOf(":muyun-common", ":muyun-ability"),
    ":muyun-platform" to setOf(":muyun-ability", ":muyun-dynamic"),
    ":muyun-iam" to setOf(":muyun-ability", ":muyun-platform"),
    ":muyun-demo" to setOf(":muyun-ability", ":muyun-platform", ":muyun-iam"),
    ":muyun-web-adapter" to setOf(":muyun-ability"),
    ":muyun-platform-web" to setOf(":muyun-platform", ":muyun-web-adapter"),
    ":muyun-iam-web" to setOf(":muyun-iam", ":muyun-web-adapter", ":muyun-platform-web"),
    ":muyun-dynamic-web" to setOf(":muyun-dynamic", ":muyun-platform", ":muyun-web-adapter", ":muyun-platform-web"),
    ":muyun-demo-web" to setOf(":muyun-demo", ":muyun-web-adapter", ":muyun-platform-web"),
    ":muyun-boot" to setOf(
        ":muyun-platform", ":muyun-iam", ":muyun-web-adapter", ":muyun-platform-web",
        ":muyun-iam-web", ":muyun-dynamic-web"
    ),
)

tasks.register("verifyModuleBoundaries") {
    description = "Verifies production Gradle dependency direction and Boot host boundaries."
    group = LifecycleBasePlugin.VERIFICATION_GROUP

    doLast {
        val violations = mutableListOf<String>()
        subprojects.forEach { sourceProject ->
            productionDependencyConfigurations.forEach { configurationName ->
                sourceProject.configurations.findByName(configurationName)
                    ?.dependencies
                    ?.withType(org.gradle.api.artifacts.ProjectDependency::class.java)
                    ?.forEach { dependency: org.gradle.api.artifacts.ProjectDependency ->
                        val targetPath = dependency.path
                        if (targetPath !in allowedProductionProjectDependencies.getValue(sourceProject.path)) {
                            violations += ("${sourceProject.path} must not depend on $targetPath "
                                    + "through production configuration $configurationName")
                        }
                        if (sourceProject.path in coreModulePaths && targetPath in deliveryModulePaths) {
                            violations += "${sourceProject.path} must not depend on delivery module $targetPath"
                        }
                        if (sourceProject.path == ":muyun-web-adapter"
                                && (targetPath in webDeliveryModulePaths || targetPath == ":muyun-boot")) {
                            violations += ":muyun-web-adapter must not depend on $targetPath"
                        }
                        if (sourceProject.path in webDeliveryModulePaths && targetPath == ":muyun-boot") {
                            violations += "${sourceProject.path} must not depend on application host :muyun-boot"
                        }
                    }
            }
        }

        val forbiddenBootStereotypes = Regex("@(RestController|Controller|Service|Repository)\\b")
        fileTree(project(":muyun-boot").file("src/main/java")) {
            include("**/*.java")
        }.files.forEach { source ->
            if (forbiddenBootStereotypes.containsMatchIn(source.readText())) {
                violations += ":muyun-boot must not declare delivery or domain stereotype: ${source.relativeTo(rootDir)}"
            }
        }

        check(violations.isEmpty()) {
            "Module boundary violations:\n${violations.joinToString("\n") { " - $it" }}"
        }
    }
}

integrationTestTasks.forEach { integrationTest ->
    integrationTest.configure {
        mustRunAfter(unitTestTasks)
    }
}

tasks.register("verifyAll") {
    description = "Runs all backend unit and integration tests across subprojects."
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    dependsOn(unitTestTasks)
    dependsOn(integrationTestTasks)
    dependsOn("verifyModuleBoundaries")
}
