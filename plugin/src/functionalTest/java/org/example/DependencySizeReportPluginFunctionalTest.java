package org.example;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.gradle.testkit.runner.GradleRunner;
import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.TaskOutcome;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class DependencySizeReportPluginFunctionalTest {
    private Path getBuildFile(Path projectDir) {
        return projectDir.resolve("build.gradle.kts");
    }

    private Path getSettingsFile(Path projectDir) {
        return projectDir.resolve("settings.gradle");
    }

    @Test
    void basicApply(@TempDir Path projectDir) throws IOException {
        writeString(getSettingsFile(projectDir), "");
        writeString(getBuildFile(projectDir),
                """
                        plugins {
                          id("java")
                          id("dependency-size-report")
                        }
                        repositories {
                          mavenCentral()
                        }
                        dependencies {
                          implementation("com.google.guava:guava:33.5.0-jre2")
                        }
                        """);

        // Run the build
        GradleRunner runner = GradleRunner.create();
        runner.forwardOutput();
        runner.withPluginClasspath();
        runner.withArguments("tasks", "--configuration-cache");
        runner.withProjectDir(projectDir.toFile());
        BuildResult result = runner.build();
        BuildResult rerunResult = runner.build();

        // Verify the result
        var task = result.task(":tasks");
        assertNotNull(task);
        assertEquals(TaskOutcome.SUCCESS, task.getOutcome());
        assertTrue(result.getOutput().contains("Configuration cache entry stored."));

        var rerunTask = rerunResult.task(":tasks");
        assertNotNull(rerunTask);
        assertEquals(TaskOutcome.SUCCESS, rerunTask.getOutcome());
        assertTrue(rerunResult.getOutput().contains("Configuration cache entry reused."));
    }

    @Test
    void canRunTask(@TempDir Path projectDir) throws IOException {
        writeString(getSettingsFile(projectDir), "");
        writeString(getBuildFile(projectDir),
                """
                        plugins {
                          id("java")
                          id("dependency-size-report")
                        }
                        repositories {
                          mavenCentral()
                        }
                        dependencies {
                          implementation("com.google.guava:guava:33.5.0-jre")
                        }
                        """);

        // Run the build
        GradleRunner runner = GradleRunner.create();
        runner.forwardOutput();
        runner.withPluginClasspath();
        runner.withArguments("dependencySize", "--configuration-cache");
        runner.withProjectDir(projectDir.toFile());
        BuildResult result = runner.build();
        BuildResult rerunResult = runner.build();

        // Verify the result
        var task = result.task(":dependencySize");
        assertNotNull(task);
        assertEquals(TaskOutcome.SUCCESS, task.getOutcome());
        assertTrue(result.getOutput().contains("Configuration cache entry stored."));

        var rerunTask = rerunResult.task(":dependencySize");
        assertNotNull(rerunTask);
        assertEquals(TaskOutcome.UP_TO_DATE, rerunTask.getOutcome());
        assertTrue(rerunResult.getOutput().contains("Configuration cache entry reused."));
    }

    @ParameterizedTest
    @ValueSource(strings = {"9.3.0-milestone-1", "9.2.1", "8.14.3"})
    public void worksOnVersions(String version, @TempDir Path projectDir) throws IOException {
        writeString(getSettingsFile(projectDir), "");
        writeString(getBuildFile(projectDir),
                """
                        plugins {
                          id("java")
                          id("dependency-size-report")
                        }
                        repositories {
                          mavenCentral()
                        }
                        dependencies {
                          implementation("com.google.guava:guava:33.5.0-jre")
                        }
                        """);

        // Run the build
        GradleRunner runner = GradleRunner.create();
        runner.forwardOutput();
        runner.withPluginClasspath();
        runner.withArguments("dependencySize");
        runner.withProjectDir(projectDir.toFile());
        runner.withGradleVersion(version);
        BuildResult result = runner.build();
        BuildResult rerunResult = runner.build();

        // Verify the result
        var task = result.task(":dependencySize");
        assertNotNull(task);
        assertEquals(TaskOutcome.SUCCESS, task.getOutcome());

        var tek = result.task(":classes");
        System.out.println(tek);
        System.out.println(result.getTasks());

        var rerunTask = rerunResult.task(":dependencySize");
        assertNotNull(rerunTask);
        assertEquals(TaskOutcome.UP_TO_DATE, rerunTask.getOutcome());
    }

    @Test
    public void multiProjectTest(@TempDir Path projectDir) throws IOException {
        writeString(getSettingsFile(projectDir), """
                include("lib")
                """);
        writeString(getBuildFile(projectDir),
                """
                        plugins {
                          id("java")
                          id("dependency-size-report-aggregation")
                          id("dependency-size-report")
                        }
                        repositories {
                          mavenCentral()
                        }
                        dependencies {
                          implementation("com.google.guava:guava:33.5.0-jre!!")
                          dependencySizeAggregation(project)
                          dependencySizeAggregation(project(":lib", "dependencySize"))
                        }
                        """);
        var libDir = projectDir.resolve("lib");
        Files.createDirectories(libDir);
        writeString(getBuildFile(libDir),
                """
                        plugins {
                          id("java")
                          id("dependency-size-report")
                        }
                        repositories {
                          mavenCentral()
                        }
                        dependencies {
                          implementation("com.google.guava:guava:33.4.8-jre!!")
                        }
                        """);

        // Run the build
        GradleRunner runner = GradleRunner.create();
        runner.forwardOutput();
        runner.withPluginClasspath();
        runner.withArguments("dependencySizeReport", "--stacktrace");
        runner.withProjectDir(projectDir.toFile());
        BuildResult result = runner.build();
        BuildResult rerunResult = runner.build();

        // Verify the result
        var task = result.task(":dependencySizeReport");
        assertNotNull(task);
        assertEquals(TaskOutcome.SUCCESS, task.getOutcome());

        System.out.println(result.getTasks());

        // var rerunTask = rerunResult.task(":dependencySizeReport");
        // assertNotNull(rerunTask);
        // assertEquals(TaskOutcome.UP_TO_DATE, rerunTask.getOutcome());
    }

    @Test
    public void recursiveTest(@TempDir Path projectDir) throws IOException {
        writeString(getSettingsFile(projectDir), """
                include("lib")
                """);
        writeString(getBuildFile(projectDir),
                """
                        plugins {
                          id("java")
                          id("dependency-size-report-aggregation")
                          id("dependency-size-report")
                        }
                        repositories {
                          mavenCentral()
                        }
                        dependencies {
                          implementation("com.google.guava:guava:33.5.0-jre!!")
                          dependencySizeAggregation(project)
                        }
                        dependencies {
                          subprojects.forEach { subproject ->
                            subproject.plugins.withId("java") {
                              subproject.plugins.apply("dependency-size-report")
                              dependencySizeAggregation(project(subproject.path, "dependencySize"))
                            }
                          }
                        }
                        """);
        var libDir = projectDir.resolve("lib");
        Files.createDirectories(libDir);
        writeString(getBuildFile(libDir),
                """
                        plugins {
                          id("java")
                          //id("dependency-size-report")
                        }
                        repositories {
                          mavenCentral()
                        }
                        dependencies {
                          implementation("com.google.guava:guava:33.4.8-jre!!")
                        }
                        """);

        // Run the build
        GradleRunner runner = GradleRunner.create();
        runner.forwardOutput();
        runner.withPluginClasspath();
        runner.withArguments("dependencySizeReport", "--stacktrace");
        runner.withProjectDir(projectDir.toFile());
        BuildResult result = runner.build();
        BuildResult rerunResult = runner.build();

        // Verify the result
        var task = result.task(":dependencySizeReport");
        assertNotNull(task);
        assertEquals(TaskOutcome.SUCCESS, task.getOutcome());

        System.out.println(result.getTasks());

        // var rerunTask = rerunResult.task(":dependencySizeReport");
        // assertNotNull(rerunTask);
        // assertEquals(TaskOutcome.UP_TO_DATE, rerunTask.getOutcome());
    }

    private void writeString(Path file, String string) throws IOException {
        Files.writeString(file, string);
    }
}
