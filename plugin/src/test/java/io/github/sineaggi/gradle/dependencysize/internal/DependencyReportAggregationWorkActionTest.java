package io.github.sineaggi.gradle.dependencysize.internal;

import org.gradle.api.Project;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.testfixtures.ProjectBuilder;
import org.gradle.workers.WorkParameters;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import static io.github.sineaggi.gradle.dependencysize.internal.DependencyReportAggregationWorkAction.humanReadableByteCountBin;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DependencyReportAggregationWorkActionTest {

    @TempDir
    Path tempDir;

    private Project project;

    @BeforeEach
    void setUp() {
        project = ProjectBuilder.builder().build();
    }

    // --- work action tests ---

    @Test
    void executeCreatesHtmlOutputFile() throws IOException {
        File proto = writeReport("project.bin", report(":app",
                holder("com.example", "alpha", "1.0", "/cache/alpha.jar", 1024)));

        Document doc = executeAndParse("report.html", proto);
        assertEquals("Dependency Size Report", doc.title());
    }

    @Test
    void executeAggregatesAcrossMultipleReports() throws IOException {
        File proto1 = writeReport("proj1.bin", report(":app",
                holder("com.example", "alpha", "1.0", "/cache/alpha.jar", 1024),
                holder("com.example", "beta", "2.0", "/cache/beta.jar", 2048)));
        File proto2 = writeReport("proj2.bin", report(":lib",
                holder("com.example", "gamma", "3.0", "/cache/gamma.jar", 512)));

        Document doc = executeAndParse("report.html", proto1, proto2);
        Elements boxes = doc.select(".stat-bar .stat-box");
        assertEquals("3", boxes.get(0).select(".stat-value").text());
    }

    @Test
    void executeDeduplicatesHoldersWithIdenticalPathSizeAndGav() throws IOException {
        // Same jar file referenced from two project reports — should count as one dep.
        HolderData shared = holder("com.example", "shared", "1.0", "/cache/shared.jar", 1024);
        File proto1 = writeReport("proj1.bin", report(":app", shared));
        File proto2 = writeReport("proj2.bin", report(":lib", shared));

        Document doc = executeAndParse("report.html", proto1, proto2);
        Elements boxes = doc.select(".stat-bar .stat-box");
        assertEquals("1", boxes.get(0).select(".stat-value").text());
    }

    @Test
    void executeSortsDuplicatesByTotalSizeDescending() throws IOException {
        // "big" GA totals 6000; "small" GA totals 1500 — big should appear first.
        File proto = writeReport("all.bin", report(":root",
                holder("com.example", "big", "1.0", "/cache/big-1.0.jar", 4000),
                holder("com.example", "big", "2.0", "/cache/big-2.0.jar", 2000),
                holder("com.example", "small", "1.0", "/cache/small-1.0.jar", 1000),
                holder("com.example", "small", "2.0", "/cache/small-2.0.jar", 500)));

        Document doc = executeAndParse("report.html", proto);
        Element dupesTab = doc.select("div.tab").get(1);
        Elements rows = dupesTab.select("tbody tr");
        assertEquals("com.example:big", rows.get(0).select("td").get(0).text());
        assertEquals("com.example:small", rows.get(1).select("td").get(0).text());
    }

    // --- humanReadableByteCountBin tests ---

    @Test
    void humanReadableFormatsBytes() {
        assertEquals("0 B", humanReadableByteCountBin(0));
        assertEquals("1 B", humanReadableByteCountBin(1));
        assertEquals("1023 B", humanReadableByteCountBin(1023));
    }

    @Test
    void humanReadableFormatsKibibytes() {
        assertEquals("1.0 KiB", humanReadableByteCountBin(1024));
        assertEquals("1.5 KiB", humanReadableByteCountBin(1536));
        assertEquals("2.0 KiB", humanReadableByteCountBin(2048));
    }

    @Test
    void humanReadableFormatsMebibytes() {
        assertEquals("1.0 MiB", humanReadableByteCountBin(1024 * 1024));
    }

    @Test
    void humanReadableFormatsGibibytes() {
        assertEquals("1.0 GiB", humanReadableByteCountBin(1024L * 1024 * 1024));
    }

    // --- helpers ---

    private Document executeAndParse(String outputName, File... inputs) throws IOException {
        File outputFile = tempDir.resolve(outputName).toFile();
        TestParameters params = new TestParameters(project);
        params.others.from((Object[]) inputs);
        params.outputFile.set(outputFile);
        new TestWorkAction(params).execute();
        return Jsoup.parse(outputFile, "UTF-8");
    }

    private File writeReport(String name, ReportData report) throws IOException {
        File f = tempDir.resolve(name).toFile();
        try (OutputStream os = Files.newOutputStream(f.toPath())) {
            ReportIO.write(report, os);
        }
        return f;
    }

    private static HolderData holder(String group, String artifact, String version, String path, long size) {
        return new HolderData("", path, size, "", group, artifact, version);
    }

    private static ReportData report(String projectPath, HolderData... holders) {
        return new ReportData(projectPath, Arrays.asList(holders));
    }

    private static class TestParameters implements DependencyReportAggregationWorkAction.Parameters {
        final ConfigurableFileCollection others;
        final RegularFileProperty outputFile;

        TestParameters(Project project) {
            this.others = project.getObjects().fileCollection();
            this.outputFile = project.getObjects().fileProperty();
        }

        @Override public ConfigurableFileCollection getOthers() { return others; }
        @Override public RegularFileProperty getOutputFile() { return outputFile; }
    }

    private static class TestWorkAction extends DependencyReportAggregationWorkAction {
        private final Parameters params;

        TestWorkAction(Parameters params) {
            this.params = params;
        }

        @Override
        public Parameters getParameters() {
            return params;
        }
    }
}
