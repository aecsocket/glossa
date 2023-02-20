plugins {
    id("java-conventions")
    kotlin("jvm")
    id("org.jlleitschuh.gradle.ktlint")
}

kotlin {
    jvmToolchain(indra.javaVersions().target().get())
}

ktlint {
    disabledRules.set(setOf("no-wildcard-imports", "indent"))
}

dependencies {
    testImplementation(kotlin("test"))
}
