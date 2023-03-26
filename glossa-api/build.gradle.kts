plugins {
    id("kotlin-conventions")
    id("publishing-conventions")
}

dependencies {
    implementation(libs.adventureApi)
    implementation(libs.adventureTextMiniMessage)
    implementation(libs.icu4j)
    implementation(libs.kotlinReflect)

    testImplementation(libs.adventureTextSerializerGson)
}
