plugins {
    `java-gradle-plugin`
    id("com.google.protobuf") version "0.9.5"
}

dependencies {
    compileOnly("com.google.protobuf:protobuf-java:4.33.1")
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
    val dependencySizeReport by plugins.creating {
        id = "dependency-size-report"
        implementationClass = "org.example.DependencySizeReportPlugin"
    }
    val dependencySizeReportAggregation by plugins.creating {
        id = "dependency-size-report-aggregation"
        implementationClass = "org.example.DependencySizeReportAggregationPlugin"
    }
}

gradlePlugin.testSourceSets.add(sourceSets["functionalTest"])

tasks.named<Task>("check") {
    dependsOn(testing.suites.named("functionalTest"))
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:4.33.1"
    }
}
