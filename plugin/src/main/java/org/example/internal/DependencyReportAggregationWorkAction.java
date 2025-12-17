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
import java.nio.file.Path;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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
                simpleDeps.add(new SimpleDep(holder.getPath(), holder.getSize(), holder.getGroup() + ":" + holder.getArtifact()));
            }
        }
        System.out.println("Total deps " + simpleDeps.size());
        System.out.println("Total dep size " + humanReadableByteCountBin(simpleDeps.stream().mapToLong(SimpleDep::size).sum()));
        System.out.println("Smallest 10");
        simpleDeps.stream().sorted(Comparator.comparingLong(SimpleDep::size).reversed()).limit(10).forEach(simpleDep -> {
            System.out.println("  " + simpleDep.ga() + " : " + humanReadableByteCountBin(simpleDep.size()));
        });        System.out.println("Largest 10");
        simpleDeps.stream().sorted(Comparator.comparingLong(SimpleDep::size)).limit(10).forEach(simpleDep -> {
            System.out.println("  " + simpleDep.ga() + " : " + humanReadableByteCountBin(simpleDep.size()));
        });

        // convert groupBy to java streams
        simpleDeps.stream().collect(Collectors.groupingBy(SimpleDep::ga)).entrySet().stream().sorted(Comparator.comparingLong((Map.Entry<String, List<SimpleDep>> entry) -> entry.getValue().stream().mapToLong(SimpleDep::size).sum()).reversed()).forEach((entry) -> {
            var ga = entry.getKey();
            var deps = entry.getValue();
            if (deps.size() > 1) {
                System.out.println("Duplicate dep " + ga + " count " + deps.size() + " total size " + humanReadableByteCountBin(deps.stream().mapToLong(SimpleDep::size).sum()));
                deps.forEach(dep -> {
                    System.out.println("   " + dep.path() + " : " + humanReadableByteCountBin(dep.size()));
                });
            }
        });
    }

    record SimpleDep(
            String path,
            long size,
            String ga
    ) {

    }

    public static String humanReadableByteCountBin(long bytes) {
        long absB = bytes == Long.MIN_VALUE ? Long.MAX_VALUE : Math.abs(bytes);
        if (absB < 1024) {
            return bytes + " B";
        }
        long value = absB;
        CharacterIterator ci = new StringCharacterIterator("KMGTPE");
        for (int i = 40; i >= 0 && absB > 0xfffccccccccccccL >> i; i -= 10) {
            value >>= 10;
            ci.next();
        }
        value *= Long.signum(bytes);
        return String.format("%.1f %ciB", value / 1024.0, ci.current());
    }
}
