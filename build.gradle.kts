plugins {
    alias(libs.plugins.spring.boot) apply false
    alias(libs.plugins.spring.dependency.management) apply false
    java
}

allprojects {
    group = "net.ximatai.muyun.spring"
    version = "0.1.0-SNAPSHOT"
}

subprojects {
    apply(plugin = "java-library")

    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(21))
        }
    }

    tasks.withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
    }

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
        maxParallelForks = 1
        forkEvery = 0
        systemProperty("junit.jupiter.execution.parallel.enabled", "false")

        if (project.name == "muyun-platform") {
            reports.html.required.set(false)
            reports.junitXml.includeSystemOutLog.set(false)
            reports.junitXml.includeSystemErrLog.set(false)
        }
    }

    dependencies {
        "compileOnly"(rootProject.libs.lombok)
        "annotationProcessor"(rootProject.libs.lombok)
        "testCompileOnly"(rootProject.libs.lombok)
        "testAnnotationProcessor"(rootProject.libs.lombok)
    }
}
