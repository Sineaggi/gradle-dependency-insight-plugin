package io.github.sineaggi.gradle.dependencysize.internal;

import io.github.sineaggi.gradle.dependencysize.tasks.DependencySizeAggregationTask;
import io.github.sineaggi.gradle.dependencysize.DependencySizeReport;
import org.gradle.api.tasks.TaskContainer;
import org.gradle.api.tasks.TaskProvider;

import javax.inject.Inject;

public class DefaultDependencySizeReport implements DependencySizeReport {
    private final String name;
    private final TaskProvider<DependencySizeAggregationTask> reportTask;

    @Inject
    public DefaultDependencySizeReport(String name, TaskContainer tasks) {
        this.name = name;
        reportTask = tasks.register("dependencySizeReport", DependencySizeAggregationTask.class, task -> {
            task.setGroup("verification");
            task.setDescription("Generates aggregated dependency size report.");
        });
    }

    @Override
    public TaskProvider<DependencySizeAggregationTask> getReportTask() {
        return reportTask;
    }

    @Override
    public String getName() {
        return name;
    }
}
