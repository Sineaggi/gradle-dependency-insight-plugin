package org.example.internal;

import com.example.protos.AggregateReports;
import com.example.protos.Holder;
import com.example.protos.Report;
import org.gradle.api.GradleException;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.workers.WorkAction;
import org.gradle.workers.WorkParameters;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public abstract class DependencyReportAggregationWorkAction implements WorkAction<DependencyReportAggregationWorkAction.WriterActionParameters> {
    public interface WriterActionParameters extends WorkParameters {
        ConfigurableFileCollection getOthers();

        RegularFileProperty getOutputFile();
    }

    @Override
    public void execute() {
        // todo: read all others, create combined report.
        List<Report> reports = getParameters().getOthers().getFiles().stream()
                .map(File::toPath)
                .map(path -> {
                    try {
                        return Report.parseFrom(Files.readAllBytes(path));
                    } catch (IOException e) {
                        throw new GradleException("failed to read report " + path, e);
                    }
                }).toList();
        //var projectHoders = reports.stream().map(report -> {
        //    ProjectHolder.newBuilder()
        //            .setHolder(holder)
        //            .build();
        //});
        AggregateReports aggregateReports = AggregateReports.newBuilder()
                .addAllReports(reports)
                .build();

        Set<SimpleDep> simpleDeps = new HashSet<>();
        for (Report report : aggregateReports.getReportsList()) {
            for (Holder holder : report.getHoldersList()) {
                simpleDeps.add(new SimpleDep(holder.getPath(), holder.getSize()));
            }
        }
        System.out.println("Total deps " + simpleDeps.size());
        System.out.println("Total dep size " + simpleDeps.stream().mapToLong(SimpleDep::size).sum() / 1000.0f / 1000.0f + "MiB");
    }

    record SimpleDep(
            String path,
            long size
    ) {

    }
}
