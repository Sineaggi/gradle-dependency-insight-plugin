dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

pluginManagement {
    includeBuild("..")
}

rootProject.name = "user"
include("app")
include("lib")

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
