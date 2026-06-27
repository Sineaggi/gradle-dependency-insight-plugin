package org.example.internal;

import com.example.protos.AggregateReports;
import com.example.protos.Holder;
import com.example.protos.Report;
import org.gradle.api.GradleException;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.workers.WorkAction;
import org.gradle.workers.WorkParameters;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.*;
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
                }).collect(Collectors.toList());
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
                simpleDeps.add(new SimpleDep(holder.getPath(), holder.getSize(), holder.getGroup() + ":" + holder.getArtifact(), holder.getVersion()));
            }
        }
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintWriter out = new PrintWriter(outputStream);
        out.println("Total deps " + simpleDeps.size());
        out.println("Total dep size " + humanReadableByteCountBin(simpleDeps.stream().mapToLong(SimpleDep::size).sum()));
        out.println("Largest 10");
        simpleDeps.stream().sorted(Comparator.comparingLong(SimpleDep::size).reversed()).limit(10).forEach(simpleDep -> {
            out.println("  " + simpleDep.gav() + " : " + humanReadableByteCountBin(simpleDep.size()));
        });
        out.println("Smallest 10");
        simpleDeps.stream().sorted(Comparator.comparingLong(SimpleDep::size)).limit(10).forEach(simpleDep -> {
            out.println("  " + simpleDep.gav() + " : " + humanReadableByteCountBin(simpleDep.size()));
        });

        // convert groupBy to java streams
        simpleDeps.stream().collect(Collectors.groupingBy(SimpleDep::ga)).entrySet().stream().sorted(Comparator.comparingLong((Map.Entry<String, List<SimpleDep>> entry) -> entry.getValue().stream().mapToLong(SimpleDep::size).sum()).reversed()).forEach((entry) -> {
            String ga = entry.getKey();
            List<SimpleDep> deps = entry.getValue();
            if (deps.size() > 1) {
                out.println("Duplicate dep " + ga + " count " + deps.size() + " total size " + humanReadableByteCountBin(deps.stream().mapToLong(SimpleDep::size).sum()));
                deps.forEach(dep -> {
                    out.println("   " + dep.path() + " : " + humanReadableByteCountBin(dep.size()));
                });
            }
        });
        out.flush();
        System.out.println(outputStream);
    }

    static final class SimpleDep {
        private final String path;
        private final long size;
        private final String ga;
        private final String version;

        SimpleDep(
                String path,
                long size,
                String ga,
                String version
        ) {
            this.path = path;
            this.size = size;
            this.ga = ga;
            this.version = version;
        }

        public String path() {
            return path;
        }

        public long size() {
            return size;
        }

        public String ga() {
            return ga;
        }

        public String version() {
            return version;
        }

        public String gav() {
            return ga + ":" + version;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) return true;
            if (obj == null || obj.getClass() != this.getClass()) return false;
            SimpleDep that = (SimpleDep) obj;
            return Objects.equals(this.path, that.path) &&
                   this.size == that.size &&
                   Objects.equals(this.ga, that.ga) &&
                   Objects.equals(this.version, that.version);
        }

        @Override
        public int hashCode() {
            return Objects.hash(path, size, ga, version);
        }

        @Override
        public String toString() {
            return "SimpleDep[" +
                   "path=" + path + ", " +
                   "size=" + size + ", " +
                   "ga=" + ga + ", " +
                   "version=" + version + ']';
        }
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
