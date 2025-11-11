dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
    }
}

rootProject.name = "gradle-dependency-size"
include("plugin")

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
