plugins {
    `java-gradle-plugin`
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
