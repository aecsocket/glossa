plugins {
    id("parent-conventions")
    id("kotlin-conventions")
    id("publishing-conventions")
}

group = "io.github.aecsocket"
version = "1.0.6-SNAPSHOT"
description = "Localization library for Adventure components"

dependencies {
    api(libs.adventureApi)
    api(libs.adventureTextMiniMessage)
    implementation(libs.icu4j)
    implementation(libs.kotlinReflect)

    testImplementation(libs.adventureTextSerializerGson)
}
