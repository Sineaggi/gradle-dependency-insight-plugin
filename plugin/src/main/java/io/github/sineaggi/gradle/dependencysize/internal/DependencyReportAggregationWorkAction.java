package io.github.sineaggi.gradle.dependencysize.internal;

import org.gradle.api.GradleException;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.reporting.HtmlReportRenderer;
import org.gradle.workers.WorkAction;
import org.gradle.workers.WorkParameters;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public abstract class DependencyReportAggregationWorkAction implements WorkAction<DependencyReportAggregationWorkAction.Parameters> {
    public interface Parameters extends WorkParameters {
        ConfigurableFileCollection getOthers();
        RegularFileProperty getOutputFile();
    }

    @Override
    public void execute() {
        List<ReportData> reports = getParameters().getOthers().getFiles().stream()
                .map(File::toPath)
                .map(path -> {
                    try (InputStream is = Files.newInputStream(path)) {
                        return ReportIO.read(is);
                    } catch (IOException e) {
                        throw new GradleException("failed to read report " + path, e);
                    }
                }).collect(Collectors.toList());

        Set<SimpleDep> simpleDeps = new HashSet<>();
        for (ReportData report : reports) {
            for (HolderData holder : report.getHoldersList()) {
                simpleDeps.add(new SimpleDep(
                        holder.getPath(),
                        holder.getSize(),
                        holder.getGroup() + ":" + holder.getArtifact(),
                        holder.getVersion()));
            }
        }

        List<SimpleDep> bySize = simpleDeps.stream()
                .sorted(Comparator.comparingLong(SimpleDep::size).reversed())
                .collect(Collectors.toList());

        List<Map.Entry<String, List<SimpleDep>>> duplicates = simpleDeps.stream()
                .collect(Collectors.groupingBy(SimpleDep::ga))
                .entrySet().stream()
                .filter(e -> e.getValue().size() > 1)
                .sorted(Comparator.comparingLong((Map.Entry<String, List<SimpleDep>> e) ->
                        e.getValue().stream().mapToLong(SimpleDep::size).sum()).reversed())
                .collect(Collectors.toList());

        AggregateReportModel model = new AggregateReportModel(
                simpleDeps.size(),
                bySize.stream().mapToLong(SimpleDep::size).sum(),
                bySize.stream().limit(10).collect(Collectors.toList()),
                bySize.stream().sorted(Comparator.comparingLong(SimpleDep::size)).limit(10).collect(Collectors.toList()),
                duplicates);

        File outputFile = getParameters().getOutputFile().get().getAsFile();
        new HtmlReportRenderer().renderSinglePage(model, new DependencyAggregateReportRenderer(), outputFile);
    }

    static String humanReadableByteCountBin(long bytes) {
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
