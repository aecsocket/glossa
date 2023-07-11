enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
  repositories {
    gradlePluginPortal()
  }
  includeBuild("build-logic")
}

plugins {
  id("org.gradle.toolchains.foojay-resolver-convention") version("0.4.0")
}

rootProject.name = "glossa-parent"

include("glossa")
include("glossa-configurate")
