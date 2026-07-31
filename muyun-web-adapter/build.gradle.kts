configurations.configureEach {
    exclude(group = "org.springframework.boot", module = "spring-boot-starter-jackson")
    exclude(group = "org.springframework.boot", module = "spring-boot-starter-jackson-test")
    exclude(group = "org.springframework.boot", module = "spring-boot-jackson")
}

dependencies {
    api(project(":muyun-ability"))

    api(libs.spring.boot.starter.web)
    api(libs.spring.boot.jackson2)
    implementation(libs.spring.boot.starter.jdbc)
    implementation(libs.spring.boot.starter.websocket)
    implementation(libs.spring.tx)
    implementation(libs.muyun.database.spring.boot.starter)

    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.spring.boot.starter.webmvc.test)
    testRuntimeOnly(libs.junit.platform.launcher)
}
