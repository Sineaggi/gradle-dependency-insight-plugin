plugins {
    `java-gradle-plugin`
    id("com.google.protobuf") version "0.10.0"
    `maven-publish`
}

dependencies {
    compileOnly(libs.protobuf.java)
}

testing {
    suites {
        val test = named<JvmTestSuite>("test") {
            useJUnitJupiter(libs.versions.junit)
        }

        register<JvmTestSuite>("functionalTest") {
            useJUnitJupiter(libs.versions.junit)
            dependencies {
                implementation(project())
            }

            targets {
                configureEach {
                    testTask.configure { shouldRunAfter(test) }
                }
            }
        }
    }
}

gradlePlugin {
    plugins.register("dependency-size-report") {
        implementationClass = "org.example.DependencySizeReportPlugin"
    }
    plugins.register("dependency-size-report-aggregation") {
        implementationClass = "org.example.DependencySizeReportAggregationPlugin"
    }
    plugins.register("dependency-size-report-lifecycle") {
        implementationClass = "org.example.DependencySizeReportLifecyclePlugin"
    }
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

tasks.compileJava {
    options.release = 8
}

gradlePlugin.testSourceSets.add(sourceSets["functionalTest"])

tasks.check {
    dependsOn(testing.suites.named("functionalTest"))
}

protobuf {
    protoc {
        artifact = libs.protoc.map { it.toString() }.get()
    }
}
