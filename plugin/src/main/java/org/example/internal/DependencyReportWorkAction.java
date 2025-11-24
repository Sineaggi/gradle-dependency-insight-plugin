package org.example.internal;

import com.example.protos.Holder;
import com.example.protos.Report;
import org.example.DependencySizeTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.SetProperty;
import org.gradle.workers.WorkAction;
import org.gradle.workers.WorkParameters;

import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

public abstract class DependencyReportWorkAction implements WorkAction<DependencyReportWorkAction.WriterActionParameters> {
    public interface WriterActionParameters extends WorkParameters {
        SetProperty<DependencySizeTask.Holder> getHolders();
        RegularFileProperty getOutputFile();
    }

    @Override
    public void execute() {
        // todo: read all others, create combined report.
        List<Holder> holders = getParameters().getHolders().get().stream().map(f -> {
            return Holder.newBuilder()
                    .build();
        }).toList();

        Report report = Report.newBuilder().addAllHolders(holders).build();

        try {
            Files.write(getParameters().getOutputFile().get().getAsFile().toPath(), report.toByteArray());
        } catch (IOException e) {
            throw new GradleException("failed to write", e);
        }
    }
}
