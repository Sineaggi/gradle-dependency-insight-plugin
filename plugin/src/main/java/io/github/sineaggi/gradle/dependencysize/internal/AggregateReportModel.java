package io.github.sineaggi.gradle.dependencysize.internal;

import java.util.List;
import java.util.Map;

final class AggregateReportModel {
    final int totalDeps;
    final long totalSize;
    final List<SimpleDep> largest;
    final List<SimpleDep> smallest;
    final List<Map.Entry<String, List<SimpleDep>>> duplicates;

    AggregateReportModel(
            int totalDeps,
            long totalSize,
            List<SimpleDep> largest,
            List<SimpleDep> smallest,
            List<Map.Entry<String, List<SimpleDep>>> duplicates) {
        this.totalDeps = totalDeps;
        this.totalSize = totalSize;
        this.largest = largest;
        this.smallest = smallest;
        this.duplicates = duplicates;
    }
}
