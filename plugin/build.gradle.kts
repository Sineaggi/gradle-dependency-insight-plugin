plugins {
    `java-gradle-plugin`
    id("com.google.protobuf") version "0.9.5"
    `maven-publish`
}

dependencies {
    compileOnly("com.google.protobuf:protobuf-java:3.25.8")
}

testing {
    suites {
        val test by existing(JvmTestSuite::class) {
            useJUnitJupiter("6.0.0")
        }

        val functionalTest by registering(JvmTestSuite::class) {
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
    val dependencySizeReport by plugins.registering {
        id = "dependency-size-report"
        implementationClass = "org.example.DependencySizeReportPlugin"
    }
    val dependencySizeReportAggregation by plugins.registering {
        id = "dependency-size-report-aggregation"
        implementationClass = "org.example.DependencySizeReportAggregationPlugin"
    }
    val dependencySizeReportLifecycle by plugins.registering {
        id = "dependency-size-report-lifecycle"
        implementationClass = "org.example.DependencySizeReportLifecyclePlugin"
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

tasks.compileJava {
    options.release = 11
}

gradlePlugin.testSourceSets.add(sourceSets["functionalTest"])

tasks.named<Task>("check") {
    dependsOn(testing.suites.named("functionalTest"))
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:3.25.8"
    }
}
