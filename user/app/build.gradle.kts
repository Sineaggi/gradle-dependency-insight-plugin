plugins {
    application
    id("io.github.sineaggi.dependency-size-report")
}

dependencies {
    // implementation(libs.guava)
    implementation("com.google.guava:guava:33.5.0-jre!!")
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

application {
    mainClass = "io.github.sineaggi.gradle.dependencysize.demo.App"
}
