plugins {
    id("java")
    id("dependency-size-report")
}

dependencies {
    // This dependency is used by the application.
    implementation(libs.guava)
    dependencySizeAggregation(projects.lib)
    dependencySizeAggregation(projects.app)
}
