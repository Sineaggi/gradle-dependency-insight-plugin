plugins {
    id("java")
    id("org.example.greeting")
}

dependencies {
    // This dependency is used by the application.
    implementation(libs.guava)

}
