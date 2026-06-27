plugins {
    id("java-library")
    id("dependency-size-report")
}

dependencies {
    implementation(libs.guava)
    implementation(projects.lib)
}

testing {
    suites {
        named<JvmTestSuite>("test") {
            useJUnitJupiter("6.0.0")
        }
    }
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}
