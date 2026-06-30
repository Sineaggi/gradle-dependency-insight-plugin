package io.github.sineaggi.gradle.dependencysize;

import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.type.ArtifactTypeDefinition;
import org.gradle.api.attributes.Category;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DependencySizeReportPluginTest {

    private Project project;

    @BeforeEach
    void setUp() {
        project = ProjectBuilder.builder().build();
        project.getPlugins().apply("io.github.sineaggi.dependency-size-report");
    }

    @Test
    void pluginRegistersTask() {
        assertNotNull(project.getTasks().findByName("dependencySize"));
    }

    @Test
    void pluginRegistersConsumableConfiguration() {
        Configuration conf = project.getConfigurations().findByName("dependencySize");
        assertNotNull(conf);
        assertTrue(conf.isCanBeConsumed());
        assertFalse(conf.isCanBeResolved());
    }

    @Test
    void dependencySizeConfigurationHasCategoryAttribute() {
        Configuration conf = project.getConfigurations().getByName("dependencySize");
        String category = conf.getAttributes()
                .getAttribute(Category.CATEGORY_ATTRIBUTE)
                .getName();
        assertEquals(DependencySizeReportPlugin.DEPENDENCY_SIZE_CATEGORY, category);
    }

    @Test
    void dependencySizeConfigurationHasArtifactTypeAttribute() {
        Configuration conf = project.getConfigurations().getByName("dependencySize");
        String artifactType = conf.getAttributes()
                .getAttribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE);
        assertEquals("binary", artifactType);
    }

    @Test
    void taskIsPublishedAsOutgoingArtifact() {
        Configuration conf = project.getConfigurations().getByName("dependencySize");
        assertFalse(conf.getOutgoing().getArtifacts().isEmpty());
    }
}
