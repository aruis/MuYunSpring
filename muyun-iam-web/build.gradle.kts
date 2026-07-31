plugins {
    id("java-test-fixtures")
}

configurations.configureEach {
    exclude(group = "org.springframework.boot", module = "spring-boot-starter-jackson")
    exclude(group = "org.springframework.boot", module = "spring-boot-starter-jackson-test")
    exclude(group = "org.springframework.boot", module = "spring-boot-jackson")
}

dependencies {
    api(project(":muyun-iam"))
    implementation(project(":muyun-web-adapter"))
    implementation(project(":muyun-platform-web"))

    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.boot.starter.websocket)
    implementation(libs.spring.tx)
    implementation(libs.muyun.database.spring.boot.starter)

    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.spring.boot.starter.webmvc.test)
    testImplementation(testFixtures(project(":muyun-iam")))
    testRuntimeOnly(libs.junit.platform.launcher)

    testFixturesImplementation(libs.spring.boot.starter.test)
}
