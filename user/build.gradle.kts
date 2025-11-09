plugins {
    id("java")
    id("dependency-size-insight")
}

dependencies {
    // This dependency is used by the application.
    implementation(libs.guava)

}
