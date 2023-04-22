plugins {
    id("kotlin-conventions")
    id("publishing-conventions")
}

dependencies {
    api(projects.glossa)
    implementation(libs.adventureSerializerConfigurate)
    api(libs.configurateCore)
    implementation(libs.configurateExtraKotlin)

    testImplementation(libs.configurateYaml)
    testImplementation(libs.adventureTextMiniMessage)
}
