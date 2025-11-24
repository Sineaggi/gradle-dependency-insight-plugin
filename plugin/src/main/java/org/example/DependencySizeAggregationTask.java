package org.example;

import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.TaskAction;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class DependencySizeAggregationTask extends DefaultTask {
    private final ConfigurableFileCollection others = getProject().getObjects().fileCollection();
    @InputFiles
    public ConfigurableFileCollection getOthers() {
        return others;
    }

    @TaskAction
    public void go() {
        List<DependencySizeTask.Holder> holders = new ArrayList<>();
        for (File file : others.getFiles()) {
            try (InputStream is = Files.newInputStream(file.toPath());
                 ObjectInputStream ois = new ObjectInputStream(is)) {
                holders.addAll((List) ois.readObject());
            } catch (IOException | ClassNotFoundException e) {
                throw new GradleException("failed to read", e);
            }
        }
        System.out.println("totals " + holders.stream().mapToLong(DependencySizeTask.Holder::size).sum() / 1000.0f / 1000.0f + "MiB");
    }
}
