package io.github.sineaggi.gradle.dependencysize;

import io.github.sineaggi.gradle.dependencysize.tasks.DependencySizeAggregationTask;
import org.gradle.api.reporting.ReportSpec;
import org.gradle.api.tasks.TaskProvider;

public interface DependencySizeReport extends ReportSpec {
    TaskProvider<DependencySizeAggregationTask> getReportTask();
}
