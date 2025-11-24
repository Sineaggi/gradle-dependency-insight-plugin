package org.example;

import org.gradle.api.NamedDomainObjectProvider;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;

public class DependencySizeBasePlugin implements Plugin<Project> {
    public static final String DEPENDENCY_SIZE_TOOLING = "dependencySizeTooling";
    @Override
    public void apply(Project project) {
        NamedDomainObjectProvider<Configuration> dependencySizeTooling = project.getConfigurations().register(DEPENDENCY_SIZE_TOOLING, conf -> {
        });
        project.getDependencies().add(dependencySizeTooling.getName(), "com.google.protobuf:protobuf-java:4.33.1");
    }
}
