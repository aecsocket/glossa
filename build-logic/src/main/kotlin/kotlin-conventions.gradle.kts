plugins {
    id("java-conventions")
    kotlin("jvm")
    id("org.jlleitschuh.gradle.ktlint")
}

kotlin {
    jvmToolchain(indra.javaVersions().target().get())
}

dependencies {
    testImplementation(kotlin("test"))
}
