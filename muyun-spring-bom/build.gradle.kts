plugins {
    `java-platform`
}

javaPlatform {
    allowDependencies()
}

dependencies {
    @Suppress("UNCHECKED_CAST")
    val publicArtifactProjectNames = rootProject.extra["publicArtifactProjectNames"] as List<String>
    constraints {
        publicArtifactProjectNames
            .filter { it != project.name }
            .forEach { api(project(":$it")) }
    }
}
