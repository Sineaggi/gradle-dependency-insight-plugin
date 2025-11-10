package org.example;

import org.gradle.api.reporting.ReportSpec;
import org.gradle.api.tasks.TaskProvider;
import org.jspecify.annotations.NonNull;

public interface DependencySizeReport extends ReportSpec {
    TaskProvider<@NonNull DependencySizeAggregationTask> getReportTask();
}
