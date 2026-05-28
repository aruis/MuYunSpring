pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenLocal()
        mavenCentral()
    }
}

rootProject.name = "MuYunSpring"

include(
    "muyun-common",
    "muyun-ability",
    "muyun-dynamic",
    "muyun-iam",
    "muyun-boot"
)
