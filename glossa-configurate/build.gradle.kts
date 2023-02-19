plugins {
    id("kotlin-conventions")
}

dependencies {
    implementation(projects.glossaCore)
    implementation(libs.adventureApi)
    implementation(libs.adventureSerializerConfigurate)
    implementation(libs.configurateCore)
    implementation(libs.configurateExtraKotlin)

    testImplementation(libs.configurateYaml)
    testImplementation(libs.adventureTextMiniMessage)
    testImplementation(libs.icu4j)
}
