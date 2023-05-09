plugins {
    id("kotlin-conventions")
    id("publishing-conventions")
}

dependencies {
    api(projects.glossa)
    implementation(libs.adventure.serializer.configurate4)
    api(libs.configurate.core)
    implementation(libs.configurate.extra.kotlin)

    testImplementation(libs.configurate.yaml)
}
