dependencies {
    api(project(":muyun-ability"))
    implementation(project(":muyun-platform"))
    implementation(project(":muyun-iam"))
    implementation(libs.muyun.database.spring.boot.starter)
    implementation(libs.spring.context)
    implementation(libs.spring.boot.starter)
    implementation(libs.spring.tx)

    testImplementation(libs.spring.boot.starter.test)
    testImplementation(testFixtures(project(":muyun-iam")))
    testImplementation(testFixtures(project(":muyun-platform")))
    testRuntimeOnly(libs.junit.platform.launcher)
}
