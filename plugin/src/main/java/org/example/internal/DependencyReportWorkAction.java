package org.example.internal;

import com.example.protos.Holder;
import com.example.protos.Report;
import org.example.tasks.DependencySizeTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.SetProperty;
import org.gradle.workers.WorkAction;
import org.gradle.workers.WorkParameters;

import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

public abstract class DependencyReportWorkAction implements WorkAction<DependencyReportWorkAction.WriterActionParameters> {
    public interface WriterActionParameters extends WorkParameters {
        SetProperty<DependencySizeTask.Holder> getHolders();
        Property<String> getProjectPath();
        RegularFileProperty getOutputFile();
    }

    @Override
    public void execute() {
        // todo: read all others, create combined report.
        List<Holder> holders = getParameters().getHolders().get().stream().map(holder -> {
            DependencySizeTask.Reference.GAV gav = (DependencySizeTask.Reference.GAV) holder.getReference().get();
            return Holder.newBuilder()
                    .setGav(gav.toString())
                    .setGroup(gav.getGroupId().get())
                    .setArtifact(gav.getArtifactId().get())
                    .setVersion(gav.getVersion().get())
                    .setConfigurationName(holder.getConfigurationName().get())
                    .setPath(holder.getPath().get())
                    .setSize(holder.getSize().get())
                    .build();
        }).toList();

        Report report = Report.newBuilder()
                .addAllHolders(holders)
                .setProjectPath(getParameters().getProjectPath().get())
                .build();

        try {
            Files.write(getParameters().getOutputFile().get().getAsFile().toPath(), report.toByteArray());
        } catch (IOException e) {
            throw new GradleException("failed to write", e);
        }
    }
}
