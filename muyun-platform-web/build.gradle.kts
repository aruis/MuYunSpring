plugins {
    id("java-test-fixtures")
}

configurations.configureEach {
    exclude(group = "org.springframework.boot", module = "spring-boot-starter-jackson")
    exclude(group = "org.springframework.boot", module = "spring-boot-starter-jackson-test")
    exclude(group = "org.springframework.boot", module = "spring-boot-jackson")
}

dependencies {
    api(project(":muyun-platform"))
    implementation(project(":muyun-web-adapter"))
    implementation(project(":muyun-iam"))

    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.boot.starter.jdbc)
    implementation(libs.spring.boot.starter.websocket)
    implementation(libs.spring.boot.jackson2)
    implementation(libs.spring.tx)
    implementation(libs.muyun.database.spring.boot.starter)

    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.spring.boot.starter.webmvc.test)
    testImplementation(project(":muyun-dynamic-web"))
    testImplementation(project(":muyun-iam-web"))
    testImplementation(testFixtures(project(":muyun-iam")))
    testImplementation(testFixtures(project(":muyun-platform")))
    testRuntimeOnly(libs.junit.platform.launcher)

    testFixturesImplementation(project(":muyun-web-adapter"))
}
