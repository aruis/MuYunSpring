dependencies {
    api(project(":muyun-common"))
    api(project(":muyun-ability"))
    api(libs.muyun.database.core)

    implementation(platform(libs.quarkus.bom))
    implementation(libs.quarkus.narayana.jta)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.assertj.core)
    testImplementation(libs.mockito.core)
    testImplementation(libs.muyun.database.quarkus)
    testImplementation(libs.postgresql)
    testImplementation(platform(libs.quarkus.bom))
    testImplementation(libs.quarkus.junit5)
    testImplementation(libs.quarkus.jdbc.postgresql)
    testImplementation(libs.testcontainers.junit.jupiter)
    testImplementation(libs.testcontainers.postgresql)
    testRuntimeOnly(libs.junit.platform.launcher)
}
