plugins {
    id("java")
    //id("io.github.sineaggi.dependency-size-report")
    id("io.github.sineaggi.dependency-size-report-aggregation")
}

dependencies {
    implementation(libs.guava)
    dependencySizeAggregation(projects.lib)
    dependencySizeAggregation(projects.app)
}
