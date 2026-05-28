dependencies {
    api(project(":muyun-ability"))

    compileOnly(libs.muyun.database.spring.boot.starter)
    compileOnly(libs.spring.context)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.assertj.core)
    testRuntimeOnly(libs.junit.platform.launcher)
}
