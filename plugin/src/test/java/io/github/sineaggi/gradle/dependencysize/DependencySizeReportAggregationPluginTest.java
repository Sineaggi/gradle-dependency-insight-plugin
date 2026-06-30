package io.github.sineaggi.gradle.dependencysize;

import io.github.sineaggi.gradle.dependencysize.tasks.DependencySizeAggregationTask;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.attributes.Category;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DependencySizeReportAggregationPluginTest {

    private Project project;

    @BeforeEach
    void setUp() {
        project = ProjectBuilder.builder().build();
        project.getPlugins().apply("io.github.sineaggi.dependency-size-report-aggregation");
    }

    @Test
    void pluginRegistersDependencyScopeConfiguration() {
        assertNotNull(project.getConfigurations().findByName("dependencySizeAggregation"));
    }

    @Test
    void pluginRegistersResolvableConfiguration() {
        Configuration conf = project.getConfigurations().findByName("aggregateDependencySizeReportResults");
        assertNotNull(conf);
        assertTrue(conf.isCanBeResolved());
        assertFalse(conf.isCanBeConsumed());
    }

    @Test
    void resolvableConfigurationHasDependencySizeCategoryAttribute() {
        Configuration conf = project.getConfigurations().getByName("aggregateDependencySizeReportResults");
        String category = conf.getAttributes()
                .getAttribute(Category.CATEGORY_ATTRIBUTE)
                .getName();
        assertEquals(DependencySizeReportPlugin.DEPENDENCY_SIZE_CATEGORY, category);
    }

    @Test
    void pluginRegistersAggregationTask() {
        assertNotNull(project.getTasks().findByName("dependencySizeReport"));
        assertInstanceOf(DependencySizeAggregationTask.class,
                project.getTasks().findByName("dependencySizeReport"));
    }

    @Test
    void aggregationTaskOutputFileConventionIsUnderDependencySizeDir() {
        DependencySizeAggregationTask task = (DependencySizeAggregationTask)
                project.getTasks().getByName("dependencySizeReport");
        String path = task.getOutputFile().get().getAsFile().getAbsolutePath();
        assertTrue(path.endsWith("dependency-size/dependencySizeReport.html"),
                "Expected path ending with dependency-size/dependencySizeReport.html, got: " + path);
    }

    private static void assertEquals(Object expected, Object actual) {
        org.junit.jupiter.api.Assertions.assertEquals(expected, actual);
    }
}
