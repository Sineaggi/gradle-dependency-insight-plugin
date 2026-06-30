plugins {
    id("java-library")
    id("io.github.sineaggi.dependency-size-report")
}

dependencies {
    // implementation(libs.guava)
    implementation("com.google.guava:guava:33.4.8-jre!!")
    implementation(projects.lib)
}

testing {
    suites {
        named<JvmTestSuite>("test") {
            useJUnitJupiter("6.1.0")
        }
    }
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}
