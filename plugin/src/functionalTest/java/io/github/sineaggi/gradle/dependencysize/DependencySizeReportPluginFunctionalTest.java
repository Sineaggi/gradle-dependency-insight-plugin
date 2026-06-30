package io.github.sineaggi.gradle.dependencysize;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import org.gradle.testkit.runner.GradleRunner;
import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.TaskOutcome;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DependencySizeReportPluginFunctionalTest {
    private Path getBuildFile(Path projectDir) {
        return projectDir.resolve("build.gradle.kts");
    }

    private Path getSettingsFile(Path projectDir) {
        return projectDir.resolve("settings.gradle");
    }

    @DisplayName("Shows simple application of the plugin works as expected.")
    @Test
    void basicApply(@TempDir Path projectDir) throws IOException {
        writeString(getSettingsFile(projectDir), "");
        writeString(getBuildFile(projectDir),
                /* language=GROOVY */
                """
                        plugins {
                          id("java")
                          id("io.github.sineaggi.dependency-size-report")
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
        runner.withArguments("tasks", "-Dorg.gradle.unsafe.isolated-projects=true");
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

    @DisplayName("Shows the dependencySize task succeeds and is UP_TO_DATE on rerun with configuration cache.")
    @Test
    void canRunTask(@TempDir Path projectDir) throws IOException {
        writeString(getSettingsFile(projectDir), "");
        writeString(getBuildFile(projectDir),
                /* language=GROOVY */
                """
                        plugins {
                          id("java")
                          id("io.github.sineaggi.dependency-size-report")
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
        runner.withArguments("dependencySize", "-Dorg.gradle.unsafe.isolated-projects=true");
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

    static Stream<Arguments> namedArguments() {
        return Stream.of(
                Arguments.of(Named.of("The latest 9 release", "9.6.0")),
                Arguments.of(Named.of("The latest 8 release", "8.14.5")),
                Arguments.of(Named.of("The first 8 release", "8.0")),
                Arguments.of(Named.of("The latest 7 release", "7.6.6")),
                Arguments.of(Named.of("The earliest 7 release supported", "7.4"))
        );
    }

    @DisplayName("Shows the dependencySize task works with configuration cache across a range of supported Gradle versions.")
    @ParameterizedTest
    @MethodSource("namedArguments")
    public void worksOnVersionsWithCC(String version, @TempDir Path projectDir) throws IOException {
        writeString(getSettingsFile(projectDir), "");
        writeString(getBuildFile(projectDir),
                /* language=GROOVY */
                """
                        plugins {
                          id("java")
                          id("io.github.sineaggi.dependency-size-report")
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
        runner.withArguments("dependencySize", "-Dorg.gradle.unsafe.isolated-projects=true", "--configuration-cache", "--stacktrace");
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

    @DisplayName("Shows aggregation works across multiple projects, with dependencies declared explicitly.")
    @Test
    public void multiProjectTest(@TempDir Path projectDir) throws IOException {
        writeString(getSettingsFile(projectDir), """
                include("lib")
                """);
        writeString(getBuildFile(projectDir),
                /* language=GROOVY */
                """
                        plugins {
                          id("java")
                          id("io.github.sineaggi.dependency-size-report-aggregation")
                          id("io.github.sineaggi.dependency-size-report")
                        }
                        repositories {
                          mavenCentral()
                        }
                        dependencies {
                          implementation("com.google.guava:guava:33.5.0-jre!!")
                          dependencySizeAggregation(project)
                          dependencySizeAggregation(project(":lib"))
                        }
                        """);
        var libDir = projectDir.resolve("lib");
        Files.createDirectories(libDir);
        writeString(getBuildFile(libDir),
                /* language=GROOVY */
                """
                        plugins {
                          id("java")
                          id("io.github.sineaggi.dependency-size-report")
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
        runner.withArguments("dependencySizeReport", "-Dorg.gradle.unsafe.isolated-projects=true", "--stacktrace");
        runner.withProjectDir(projectDir.toFile());
        BuildResult result = runner.build();
        BuildResult rerunResult = runner.build();

        // Verify the result
        var task = result.task(":dependencySizeReport");
        assertNotNull(task);
        assertEquals(TaskOutcome.SUCCESS, task.getOutcome());
        assertTrue(result.getOutput().contains("Configuration cache entry stored."));

        System.out.println(result.getTasks());

        var rerunTask = rerunResult.task(":dependencySizeReport");
        assertNotNull(rerunTask);
        assertEquals(TaskOutcome.UP_TO_DATE, rerunTask.getOutcome());
        assertTrue(rerunResult.getOutput().contains("Configuration cache entry reused."));
    }

    @DisplayName("Shows that a non project-isolation-safe build can successfully use this plugin.")
    @Test
    public void recursiveTest(@TempDir Path projectDir) throws IOException {
        writeString(getSettingsFile(projectDir), """
                include("lib")
                """);
        writeString(getBuildFile(projectDir),
                /* language=GROOVY */
                """
                        plugins {
                          id("java")
                          id("io.github.sineaggi.dependency-size-report-aggregation")
                          id("io.github.sineaggi.dependency-size-report")
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
                              subproject.plugins.apply("io.github.sineaggi.dependency-size-report")
                              dependencySizeAggregation(subproject)
                            }
                          }
                        }
                        """);
        var libDir = projectDir.resolve("lib");
        Files.createDirectories(libDir);
        writeString(getBuildFile(libDir),
                /* language=GROOVY */
                """
                        plugins {
                          id("java")
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

        var rerunTask = rerunResult.task(":dependencySizeReport");
        assertNotNull(rerunTask);
        assertEquals(TaskOutcome.UP_TO_DATE, rerunTask.getOutcome());
    }

    @DisplayName("Shows aggregation works under isolated projects when plugin application is handled via gradle.lifecycle.beforeProject in settings.")
    @Test
    public void recursiveIsolationCompatibleTest(@TempDir Path projectDir) throws IOException {
        writeString(getSettingsFile(projectDir),
                /* language=GROOVY */
                """
                        plugins {
                          id("io.github.sineaggi.dependency-size-report") apply false
                        }
                        include("lib")
                        gradle.lifecycle.beforeProject {
                          plugins.withId("java") {
                            plugins.apply("io.github.sineaggi.dependency-size-report")
                          }
                        }
                        """);
        writeString(getBuildFile(projectDir),
                /* language=GROOVY */
                """
                        plugins {
                          id("java")
                          id("io.github.sineaggi.dependency-size-report-aggregation")
                          id("io.github.sineaggi.dependency-size-report")
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
                            dependencySizeAggregation(subproject)
                          }
                        }
                        """);
        var libDir = projectDir.resolve("lib");
        Files.createDirectories(libDir);
        writeString(getBuildFile(libDir),
                /* language=GROOVY */
                """
                        plugins {
                          id("java")
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
        runner.withArguments("dependencySizeReport", "--stacktrace", "-Dorg.gradle.unsafe.isolated-projects=true");
        runner.withProjectDir(projectDir.toFile());
        BuildResult result = runner.build();
        BuildResult rerunResult = runner.build();

        // Verify the result
        var task = result.task(":dependencySizeReport");
        assertNotNull(task);
        assertEquals(TaskOutcome.SUCCESS, task.getOutcome());

        System.out.println(result.getTasks());

        var rerunTask = rerunResult.task(":dependencySizeReport");
        assertNotNull(rerunTask);
        assertEquals(TaskOutcome.UP_TO_DATE, rerunTask.getOutcome());
    }

    @DisplayName("Shows aggregation works under isolated projects when the dependency-size-report-lifecycle plugin handles plugin application in settings.")
    @Test
    public void recursiveIsolationLifecycleTest(@TempDir Path projectDir) throws IOException {
        writeString(getSettingsFile(projectDir),
                /* language=GROOVY */
                """
                        plugins {
                          id("io.github.sineaggi.dependency-size-report") apply false
                          id("io.github.sineaggi.dependency-size-report-lifecycle")
                        }
                        include("lib")
                        """);
        writeString(getBuildFile(projectDir),
                /* language=GROOVY */
                """
                        plugins {
                          id("java")
                          id("io.github.sineaggi.dependency-size-report-aggregation")
                          id("io.github.sineaggi.dependency-size-report")
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
                            dependencySizeAggregation(subproject)
                          }
                        }
                        """);
        var libDir = projectDir.resolve("lib");
        Files.createDirectories(libDir);
        writeString(getBuildFile(libDir),
                /* language=GROOVY */
                """
                        plugins {
                          id("java")
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
        runner.withArguments("dependencySizeReport", "--stacktrace", "-Dorg.gradle.unsafe.isolated-projects=true");
        runner.withProjectDir(projectDir.toFile());
        BuildResult result = runner.build();
        BuildResult rerunResult = runner.build();

        // Verify the result
        var task = result.task(":dependencySizeReport");
        assertNotNull(task);
        assertEquals(TaskOutcome.SUCCESS, task.getOutcome());

        System.out.println(result.getTasks());

        var rerunTask = rerunResult.task(":dependencySizeReport");
        assertNotNull(rerunTask);
        assertEquals(TaskOutcome.UP_TO_DATE, rerunTask.getOutcome());
    }

    @DisplayName("Shows that a subproject without the dependency-size-report plugin is skipped in aggregation without error.")
    @Test
    public void recursiveIsolationCompatibleErrorTest(@TempDir Path projectDir) throws IOException {
        writeString(getSettingsFile(projectDir),
                /* language=GROOVY */
                """
                        plugins {
                          id("io.github.sineaggi.dependency-size-report") apply false
                        }
                        include("lib")
                        gradle.lifecycle.beforeProject {
                          plugins.withId("java") {
                            plugins.apply("io.github.sineaggi.dependency-size-report")
                          }
                        }
                        """);
        writeString(getBuildFile(projectDir),
                /* language=GROOVY */
                """
                        plugins {
                          id("java")
                          id("io.github.sineaggi.dependency-size-report-aggregation")
                          id("io.github.sineaggi.dependency-size-report")
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
                            dependencySizeAggregation(subproject)
                          }
                        }
                        """);
        var libDir = projectDir.resolve("lib");
        Files.createDirectories(libDir);
        writeString(getBuildFile(libDir),
                /* language=GROOVY */
                """
                        plugins {
                        }
                        repositories {
                          mavenCentral()
                        }
                        dependencies {
                        }
                        """);

        // Run the build
        GradleRunner runner = GradleRunner.create();
        runner.forwardOutput();
        runner.withPluginClasspath();
        runner.withArguments("dependencySizeReport", "--stacktrace", "-Dorg.gradle.unsafe.isolated-projects=true");
        runner.withProjectDir(projectDir.toFile());
        BuildResult result = runner.build();
        BuildResult rerunResult = runner.build();

        // Verify the result
        var task = result.task(":dependencySizeReport");
        assertNotNull(task);
        assertEquals(TaskOutcome.SUCCESS, task.getOutcome());

        System.out.println(result.getTasks());

        var rerunTask = rerunResult.task(":dependencySizeReport");
        assertNotNull(rerunTask);
        assertEquals(TaskOutcome.UP_TO_DATE, rerunTask.getOutcome());
    }

    private void writeString(Path file, String string) throws IOException {
        Files.writeString(file, string);
    }
}
