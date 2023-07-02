plugins {
    id("kotlin-conventions")
    id("publishing-conventions")
}

dependencies {
    api(libs.adventure.api)
    api(libs.adventure.text.minimessage)
    implementation(libs.icu4j)
    implementation(libs.kotlin.reflect)

    testImplementation(libs.adventure.text.serializer.gson)
}
