package io.github.sineaggi.gradle.dependencysize;

import org.gradle.api.Plugin;
import org.gradle.api.initialization.Settings;

public class DependencySizeReportLifecyclePlugin implements Plugin<Settings> {
    @Override
    public void apply(Settings settings) {
        settings.getGradle().getLifecycle().beforeProject(project -> {
            project.getPluginManager().apply(DependencySizeReportPlugin.class);
        });
    }
}
