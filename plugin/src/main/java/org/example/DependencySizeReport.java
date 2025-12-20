package org.example;

import org.example.tasks.DependencySizeAggregationTask;
import org.gradle.api.reporting.ReportSpec;
import org.gradle.api.tasks.TaskProvider;
import org.jspecify.annotations.NonNull;

public interface DependencySizeReport extends ReportSpec {
    TaskProvider<@NonNull DependencySizeAggregationTask> getReportTask();
}
