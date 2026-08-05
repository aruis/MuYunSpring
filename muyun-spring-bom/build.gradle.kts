plugins {
    `java-platform`
}

javaPlatform {
    allowDependencies()
}

dependencies {
    constraints {
        rootProject.subprojects
            .filter { it.name.startsWith("muyun-") && it.name !in setOf("muyun-boot", "muyun-demo", "muyun-demo-web", "muyun-spring-bom") }
            .forEach { api(project(it.path)) }
    }
}
