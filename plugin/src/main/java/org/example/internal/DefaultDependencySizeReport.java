package org.example.internal;

import org.example.DependencySizeAggregationTask;
import org.example.DependencySizeReport;
import org.gradle.api.tasks.TaskContainer;
import org.gradle.api.tasks.TaskProvider;
import org.jspecify.annotations.NonNull;

import javax.inject.Inject;

public class DefaultDependencySizeReport implements DependencySizeReport {
    private final String name;
    private final TaskProvider<@NonNull DependencySizeAggregationTask> reportTask;

    @Inject
    public DefaultDependencySizeReport(String name, TaskContainer tasks) {
        this.name = name;
        reportTask = tasks.register("dependencySizeReport", DependencySizeAggregationTask.class, task -> {
            task.setGroup("verification");
            task.setDescription("Generates aggregated dependency size report.");
        });
    }

    @Override
    public TaskProvider<@NonNull DependencySizeAggregationTask> getReportTask() {
        return reportTask;
    }

    @Override
    public @NonNull String getName() {
        return name;
    }
}
