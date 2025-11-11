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
        val test by existing(JvmTestSuite::class) {
            useJUnitJupiter("6.0.0")
        }
    }
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}
