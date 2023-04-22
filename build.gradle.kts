plugins {
    id("parent-conventions")
    id("kotlin-conventions")
    id("publishing-conventions")
}

group = "io.github.aecsocket"
version = "1.0.5"
description = "Localization library for Adventure components"

dependencies {
    api(libs.adventureApi)
    api(libs.adventureTextMiniMessage)
    implementation(libs.icu4j)
    implementation(libs.kotlinReflect)

    testImplementation(libs.adventureTextSerializerGson)
}
