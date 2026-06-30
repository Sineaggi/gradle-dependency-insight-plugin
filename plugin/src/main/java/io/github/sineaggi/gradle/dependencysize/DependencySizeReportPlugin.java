package io.github.sineaggi.gradle.dependencysize;

import io.github.sineaggi.gradle.dependencysize.tasks.DependencySizeTask;
import org.gradle.api.NamedDomainObjectProvider;
import org.gradle.api.Project;
import org.gradle.api.Plugin;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.type.ArtifactTypeDefinition;
import org.gradle.api.attributes.Category;
import org.gradle.api.tasks.TaskProvider;

@SuppressWarnings("unused")
public abstract class DependencySizeReportPlugin implements Plugin<Project> {

    public static final String DEPENDENCY_SIZE_CONFIGURATION_NAME = "dependencySize";
    public static final String DEPENDENCY_SIZE_CATEGORY = "dependency-size-data";

    public void apply(Project project) {
        project.getPlugins().apply(DependencySizeBasePlugin.class);
        NamedDomainObjectProvider<Configuration> dependencySizeConfiguration = project.getConfigurations().register(DEPENDENCY_SIZE_CONFIGURATION_NAME, (conf) -> {
            conf.setCanBeConsumed(true);
            conf.setCanBeResolved(false);
            conf.attributes(attributes -> {
                attributes.attribute(Category.CATEGORY_ATTRIBUTE, project.getObjects().named(Category.class, DEPENDENCY_SIZE_CATEGORY));
                attributes.attribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE, "binary");
            });
        });
        NamedDomainObjectProvider<Configuration> dependencySizeTooling = project.getConfigurations().named("dependencySizeTooling");
        TaskProvider<DependencySizeTask> dependencySizeTask = project.getTasks().register("dependencySize", DependencySizeTask.class, task -> {
            task.getWorkerClasspath().from(dependencySizeTooling);
        });
        project.artifacts(artifactHandler -> {
            artifactHandler.add(dependencySizeConfiguration.getName(), dependencySizeTask);
        });
    }
}
