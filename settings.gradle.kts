dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
    }
}

plugins {
    id("com.google.protobuf") version "0.9.5" apply false
}

rootProject.name = "gradle-dependency-size"
include("plugin")

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
