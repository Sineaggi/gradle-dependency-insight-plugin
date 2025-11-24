package org.example;

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
        //List<DependencySizeTask.Holder> holders = new ArrayList<>();
        //for (File file : others.getFiles()) {
        //    try (InputStream is = Files.newInputStream(file.toPath());
        //         ObjectInputStream ois = new ObjectInputStream(is)) {
        //        holders.addAll((List) ois.readObject());
        //    } catch (IOException | ClassNotFoundException e) {
        //        throw new GradleException("failed to read", e);
        //    }
        //}
        WorkerExecutor workerExecutor = getWorkerExecutor();
        workerExecutor.classLoaderIsolation(spec -> {
            spec.getClasspath().from(getWorkerClasspath());
        }).submit(DependencyReportAggregationWorkAction.class, parameters -> {
            parameters.getOthers().from(getOthers());
            //parameters.getOutputFile().convention(getProject().getLayout().getBuildDirectory().file("reports/combined/yolo.txt"));
        });
        //System.out.println("totals " + holders.stream().mapToLong(DependencySizeTask.Holder::size).sum() / 1000.0f / 1000.0f + "MiB");
    }
}
