plugins {
    `java-gradle-plugin`
    id("com.google.protobuf") version "0.10.0"
    `maven-publish`
}

dependencies {
    compileOnly("com.google.protobuf:protobuf-java:3.25.8")
}

testing {
    suites {
        val test = named<JvmTestSuite>("test") {
            useJUnitJupiter("6.0.0")
        }

        val functionalTest = register<JvmTestSuite>("functionalTest") {
            useJUnitJupiter("6.0.0")
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
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

tasks.compileJava {
    options.release = 8
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
