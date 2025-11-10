package org.example;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.tasks.InputFiles;

public class DependencySizeAggregationTask extends DefaultTask {
    private final ConfigurableFileCollection others = getProject().getObjects().fileCollection();
    @InputFiles
    public ConfigurableFileCollection getOthers() {
        return others;
    }
}
