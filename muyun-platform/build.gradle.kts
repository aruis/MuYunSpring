dependencies {
    api(project(":muyun-ability"))
    api(project(":muyun-dynamic"))

    implementation(libs.jackson.databind)
    implementation(libs.jackson.datatype.jsr310)
    implementation(libs.poi.ooxml)

    implementation(platform(libs.quarkus.bom))
    implementation(libs.quarkus.arc)
    implementation(libs.quarkus.narayana.jta)
    implementation(libs.quarkus.scheduler)
    compileOnly(libs.muyun.database.quarkus)

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
