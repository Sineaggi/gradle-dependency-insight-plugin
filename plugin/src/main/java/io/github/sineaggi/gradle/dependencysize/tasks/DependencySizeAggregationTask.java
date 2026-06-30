package io.github.sineaggi.gradle.dependencysize.tasks;

import io.github.sineaggi.gradle.dependencysize.internal.DependencyReportAggregationWorkAction;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;
import org.gradle.workers.WorkerExecutor;

import javax.inject.Inject;

public abstract class DependencySizeAggregationTask extends DefaultTask {
    private final ConfigurableFileCollection others = getProject().getObjects().fileCollection();

    @InputFiles
    public ConfigurableFileCollection getOthers() {
        return others;
    }

    @Inject
    abstract public WorkerExecutor getWorkerExecutor();

    @Classpath
    public abstract ConfigurableFileCollection getWorkerClasspath();

    @OutputFile
    public abstract RegularFileProperty getOutputFile();

    @TaskAction
    public void go() {
        getWorkerExecutor().classLoaderIsolation(spec -> {
            spec.getClasspath().from(getWorkerClasspath());
        }).submit(DependencyReportAggregationWorkAction.class, parameters -> {
            parameters.getOthers().from(getOthers());
            parameters.getOutputFile().set(getOutputFile());
        });
    }
}
