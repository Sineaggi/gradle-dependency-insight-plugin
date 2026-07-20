package io.github.sineaggi.gradle.dependencysize.internal;

import java.util.List;

final class ReportData {
    private final String projectPath;
    private final List<HolderData> holders;

    ReportData(String projectPath, List<HolderData> holders) {
        this.projectPath = projectPath;
        this.holders = holders;
    }

    String getProjectPath() { return projectPath; }
    List<HolderData> getHoldersList() { return holders; }
}
