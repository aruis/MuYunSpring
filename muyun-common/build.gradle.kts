dependencies {
    api(libs.muyun.database.core)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.assertj.core)
    testImplementation(libs.jackson.databind)
    testImplementation(libs.swagger.parser.v3)
    testRuntimeOnly(libs.junit.platform.launcher)
}
