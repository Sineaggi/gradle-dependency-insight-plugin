package io.github.sineaggi.gradle.dependencysize.internal;

import io.github.sineaggi.gradle.dependencysize.tasks.DependencySizeTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.SetProperty;
import org.gradle.workers.WorkAction;
import org.gradle.workers.WorkParameters;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.List;
import java.util.stream.Collectors;

public abstract class DependencyReportWorkAction implements WorkAction<DependencyReportWorkAction.WriterActionParameters> {
    public interface WriterActionParameters extends WorkParameters {
        SetProperty<DependencySizeTask.Holder> getHolders();
        Property<String> getProjectPath();
        RegularFileProperty getOutputFile();
    }

    @Override
    public void execute() {
        // todo: read all others, create combined report.
        List<HolderData> holders = getParameters().getHolders().get().stream().map(holder -> {
            DependencySizeTask.Reference.GAV gav = (DependencySizeTask.Reference.GAV) holder.getReference().get();
            return new HolderData(
                    holder.getConfigurationName().get(),
                    holder.getPath().get(),
                    holder.getSize().get(),
                    gav.toString(),
                    gav.getGroupId().get(),
                    gav.getArtifactId().get(),
                    gav.getVersion().get());
        }).collect(Collectors.toList());

        ReportData report = new ReportData(getParameters().getProjectPath().get(), holders);

        try (OutputStream os = Files.newOutputStream(getParameters().getOutputFile().get().getAsFile().toPath())) {
            ReportIO.write(report, os);
        } catch (IOException e) {
            throw new GradleException("failed to write", e);
        }
    }
}
