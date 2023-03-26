plugins {
    id("kotlin-conventions")
    id("publishing-conventions")
}

dependencies {
    implementation(projects.glossaApi)
    implementation(libs.adventureApi)
    implementation(libs.adventureSerializerConfigurate)
    implementation(libs.configurateCore)
    implementation(libs.configurateExtraKotlin)

    testImplementation(libs.configurateYaml)
    testImplementation(libs.adventureTextMiniMessage)
}
