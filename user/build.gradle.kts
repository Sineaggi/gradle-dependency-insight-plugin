plugins {
    id("java")
    id("dependency-size-report")
    id("dependency-size-report-aggregation")
}

dependencies {
    implementation(libs.guava)
    dependencySizeAggregation(projects.lib)
    dependencySizeAggregation(projects.app)
}
