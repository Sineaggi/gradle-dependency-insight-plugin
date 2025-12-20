package org.example.tasks;

import org.example.internal.DependencyReportAggregationWorkAction;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.InputFiles;
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

    @TaskAction
    public void go() {
        WorkerExecutor workerExecutor = getWorkerExecutor();
        workerExecutor.classLoaderIsolation(spec -> {
            spec.getClasspath().from(getWorkerClasspath());
        }).submit(DependencyReportAggregationWorkAction.class, parameters -> {
            parameters.getOthers().from(getOthers());
            //parameters.getOutputFile().convention(getProject().getLayout().getBuildDirectory().file("reports/combined/yolo.txt"));
        });
    }
}
