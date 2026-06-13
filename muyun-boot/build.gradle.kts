plugins {
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
}

dependencies {
    implementation(project(":muyun-platform"))
    implementation(project(":muyun-iam"))
    implementation(project(":muyun-migration"))
    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.tx)
    implementation(libs.muyun.database.spring.boot.starter)

    testImplementation(libs.spring.boot.starter.test)
}
