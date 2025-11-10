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

rootProject.name = "gradle-dependency-size-user"
include("app")
include("lib")

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
