package io.github.sineaggi.gradle.dependencysize;

import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DependencySizeBasePluginTest {

    private Project project;

    @BeforeEach
    void setUp() {
        project = ProjectBuilder.builder().build();
        project.getPlugins().apply(DependencySizeBasePlugin.class);
    }

    @Test
    void pluginRegistersToolingConfiguration() {
        assertNotNull(project.getConfigurations().findByName("dependencySizeTooling"));
    }

    @Test
    void toolingConfigurationIsNotConsumable() {
        Configuration conf = project.getConfigurations().getByName("dependencySizeTooling");
        assertFalse(conf.isCanBeConsumed());
    }

    @Test
    void toolingConfigurationDeclaresProtobufDependency() {
        Configuration conf = project.getConfigurations().getByName("dependencySizeTooling");
        boolean found = conf.getDependencies().stream()
                .anyMatch(dep -> "com.google.protobuf".equals(dep.getGroup())
                        && "protobuf-java".equals(dep.getName())
                        && "3.25.8".equals(dep.getVersion()));
        assertTrue(found, "Expected com.google.protobuf:protobuf-java:3.25.8 in dependencySizeTooling");
    }
}
