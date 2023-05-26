plugins {
    id("parent-conventions")
    id("kotlin-conventions")
    id("publishing-conventions")
}

group = "io.github.aecsocket"
version = "1.0.6"
description = "Localization library for Adventure components"

dependencies {
    api(libs.adventure.api)
    api(libs.adventure.text.minimessage)
    implementation(libs.icu4j)
    implementation(libs.kotlin.reflect)

    testImplementation(libs.adventure.text.serializer.gson)
}
