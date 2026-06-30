import org.gradle.plugin.compatibility.compatibility

plugins {
    `java-gradle-plugin`
    id("com.google.protobuf")
    `maven-publish`
    id("com.gradle.plugin-publish")
    id("org.gradle.plugin-compatibility")
}

dependencies {
    compileOnly(libs.protobuf.java)
}

testing {
    suites {
        val test = named<JvmTestSuite>("test") {
            useJUnitJupiter(libs.versions.junit)
            dependencies {
                implementation(libs.jsoup)
                implementation(libs.protobuf.java)
            }
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
    website.set("https://github.com/Sineaggi/gradle-dependency-insight-plugin")
    vcsUrl.set("https://github.com/Sineaggi/gradle-dependency-insight-plugin")
    plugins {
        register("io.github.sineaggi.dependency-size-report") {
            implementationClass = "io.github.sineaggi.gradle.dependencysize.DependencySizeReportPlugin"
            displayName = "Gradle Dependency Size plugin"
            description = "Gradle plugin to generate dependency size report"
            tags.set(listOf("dependency", "size", "report", "plugin"))
            compatibility {
                features {
                    configurationCache = true
                }
            }
        }
        register("io.github.sineaggi.dependency-size-report-aggregation") {
            implementationClass = "io.github.sineaggi.gradle.dependencysize.DependencySizeReportAggregationPlugin"
            displayName = "Gradle Dependency Size aggregation plugin"
            description = "Gradle plugin to aggregate dependency size report"
            tags.set(listOf("dependency", "size", "report", "plugin"))
            compatibility {
                features {
                    configurationCache = true
                }
            }
        }
        register("io.github.sineaggi.dependency-size-report-lifecycle") {
            implementationClass = "io.github.sineaggi.gradle.dependencysize.DependencySizeReportLifecyclePlugin"
            displayName = "Gradle Dependency Size lifecycle plugin"
            description = "Gradle plugin for lifecycle dependency application"
            tags.set(listOf("dependency", "size", "report", "plugin"))
            compatibility {
                features {
                    configurationCache = true
                }
            }
        }
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
