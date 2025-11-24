package org.example.internal;

import com.example.protos.AggregateReports;
import com.example.protos.Report;
import org.gradle.api.GradleException;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.workers.WorkAction;
import org.gradle.workers.WorkParameters;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

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
        System.out.println("Found reports " + reports.size());
        System.out.println("Found reports " + aggregateReports);
    }
}
