plugins {
    alias(libs.plugins.quarkus)
}

dependencies {
    implementation(project(":muyun-platform"))
    implementation(project(":muyun-iam"))
    implementation(platform(libs.quarkus.bom))
    implementation(libs.quarkus.arc)
    implementation(libs.quarkus.rest.jackson)
    implementation(libs.quarkus.jdbc.postgresql)
    implementation(libs.quarkus.narayana.jta)
    implementation(libs.quarkus.scheduler)
    implementation(libs.jakarta.servlet.api)
    implementation(libs.muyun.database.quarkus)
    runtimeOnly(libs.postgresql)

    testImplementation(platform(libs.quarkus.bom))
    testImplementation(libs.quarkus.junit5)
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.assertj.core)
    testImplementation(libs.mockito.core)
    testImplementation(libs.testcontainers.junit.jupiter)
    testImplementation(libs.testcontainers.postgresql)
}

tasks.named<JavaCompile>("compileTestJava").configure {
    exclude(
        "**/WorkflowRuntimeAdminWebControllerTest.java",
        "**/WorkflowConfigurationWebControllerTest.java",
        "**/WorkflowDelegationWebControllerTest.java",
        "**/WorkflowHistoryWebControllerTest.java",
        "**/IamWebControllerTest.java",
        "**/LoginWebControllerTest.java",
        "**/UserAccountWebControllerTest.java",
        "**/CodeRuleWebControllerTest.java",
        "**/PlatformConfigurationWebControllerTest.java",
        "**/MenuWebControllerTest.java",
        "**/RecordLinkageRuleWebControllerTest.java",
        "**/MeasureUnitConversionRuleWebControllerTest.java",
        "**/PlatformModuleRuntimeContextWebControllerTest.java",
        "**/LowCodeGovernanceWebControllerTest.java",
        "**/CurrencyWebControllerTest.java",
        "**/DynamicPageBootstrapWebControllerTest.java",
        "**/PlatformPagePreferenceWebControllerTest.java",
        "**/DynamicExchangeTemplateWebControllerTest.java",
        "**/DynamicImportWebControllerTest.java",
        "**/DynamicExportWebControllerTest.java",
        "**/DynamicModuleTaskWebControllerTest.java"
    )
}
