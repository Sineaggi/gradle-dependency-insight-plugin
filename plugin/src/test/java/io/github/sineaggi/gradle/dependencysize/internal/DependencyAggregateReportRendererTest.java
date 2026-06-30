package io.github.sineaggi.gradle.dependencysize.internal;

import org.gradle.reporting.HtmlReportRenderer;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DependencyAggregateReportRendererTest {

    @TempDir
    Path tempDir;

    private Document render(AggregateReportModel model) throws IOException {
        File outputFile = tempDir.resolve("report.html").toFile();
        new HtmlReportRenderer().renderSinglePage(model, new DependencyAggregateReportRenderer(), outputFile);
        return Jsoup.parse(outputFile, "UTF-8");
    }

    @Test
    void pageHasCorrectTitle() throws IOException {
        Document doc = render(emptyModel());
        assertEquals("Dependency Size Report", doc.title());
        assertEquals("Dependency Size Report", doc.select("h1").text());
    }

    @Test
    void statBarDisplaysTotals() throws IOException {
        List<SimpleDep> deps = Arrays.asList(
                new SimpleDep(":app", 2048, "com.example:alpha", "1.0"),
                new SimpleDep(":lib", 1024, "com.example:beta", "2.0"));
        AggregateReportModel model = new AggregateReportModel(2, 3072, deps, deps, Collections.emptyList());

        Elements boxes = render(model).select(".stat-bar .stat-box");
        assertEquals(3, boxes.size());

        assertStatBox(boxes.get(0), "Total Dependencies", "2");
        assertStatBox(boxes.get(1), "Total Size", "3.0 KiB");
        assertStatBox(boxes.get(2), "Duplicate GAs", "0");
    }

    @Test
    void tabLabelsAreRendered() throws IOException {
        Elements links = render(emptyModel()).select("div.tab-container > ul > li > a");
        assertEquals(2, links.size());
        assertEquals("Size Overview", links.get(0).text());
        assertEquals("Duplicate Versions", links.get(1).text());
    }

    @Test
    void sizeOverviewShowsLargestTable() throws IOException {
        List<SimpleDep> largest = Arrays.asList(
                new SimpleDep(":app", 4096, "com.big:alpha", "1.0"),
                new SimpleDep(":lib", 2048, "com.big:beta", "2.0"));
        List<SimpleDep> smallest = Collections.singletonList(
                new SimpleDep(":app", 64, "com.small:gamma", "3.0"));
        AggregateReportModel model = new AggregateReportModel(3, 6208, largest, smallest, Collections.emptyList());

        Element sizeTab = render(model).select("div.tab").get(0);
        Elements rows = sizeTab.select("table").get(0).select("tbody tr");

        assertEquals(2, rows.size());
        assertDepRow(rows.get(0), "com.big:alpha", "1.0", ":app", "4.0 KiB");
        assertDepRow(rows.get(1), "com.big:beta", "2.0", ":lib", "2.0 KiB");
    }

    @Test
    void sizeOverviewShowsSmallestTable() throws IOException {
        List<SimpleDep> largest = Collections.singletonList(
                new SimpleDep(":app", 4096, "com.big:alpha", "1.0"));
        List<SimpleDep> smallest = Arrays.asList(
                new SimpleDep(":app", 128, "com.small:gamma", "3.0"),
                new SimpleDep(":lib", 64, "com.small:delta", "4.0"));
        AggregateReportModel model = new AggregateReportModel(3, 4288, largest, smallest, Collections.emptyList());

        Element sizeTab = render(model).select("div.tab").get(0);
        Elements rows = sizeTab.select("table").get(1).select("tbody tr");

        assertEquals(2, rows.size());
        assertDepRow(rows.get(0), "com.small:gamma", "3.0", ":app", "128 B");
        assertDepRow(rows.get(1), "com.small:delta", "4.0", ":lib", "64 B");
    }

    @Test
    void duplicateVersionsTabShowsOneRowPerDuplicateGa() throws IOException {
        List<SimpleDep> dupes = Arrays.asList(
                new SimpleDep(":app", 2048, "com.example:shared", "1.0"),
                new SimpleDep(":lib", 1024, "com.example:shared", "2.0"));
        AggregateReportModel model = new AggregateReportModel(
                2, 3072, dupes, dupes,
                Collections.singletonList(new AbstractMap.SimpleEntry<>("com.example:shared", dupes)));

        Element dupesTab = render(model).select("div.tab").get(1);
        Elements rows = dupesTab.select("tbody tr");

        assertEquals(1, rows.size());
        Elements cells = rows.get(0).select("td");
        assertEquals("com.example:shared", cells.get(0).text());
        assertEquals("3.0 KiB", cells.get(1).text());
        assertEquals("2", cells.get(2).text());
        assertTrue(cells.get(3).text().contains(":app (1.0)"));
        assertTrue(cells.get(3).text().contains(":lib (2.0)"));
    }

    @Test
    void duplicateVersionsTabShowsMessageWhenEmpty() throws IOException {
        Element dupesTab = render(emptyModel()).select("div.tab").get(1);
        assertEquals("No duplicate dependencies found.", dupesTab.select("p").text());
    }

    @Test
    void staticAssetsAreCopiedAlongsideHtmlFile() throws IOException {
        render(emptyModel());
        File reportsDir = tempDir.toFile();
        assertTrue(new File(reportsDir, "css/base-style.css").exists());
        assertTrue(new File(reportsDir, "css/dependency-size-report.css").exists());
        assertTrue(new File(reportsDir, "js/report.js").exists());
    }

    private static AggregateReportModel emptyModel() {
        return new AggregateReportModel(0, 0,
                Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
    }

    private static void assertStatBox(Element box, String expectedLabel, String expectedValue) {
        assertEquals(expectedLabel, box.select(".stat-label").text());
        assertEquals(expectedValue, box.select(".stat-value").text());
    }

    private static void assertDepRow(Element row, String ga, String version, String path, String size) {
        Elements cells = row.select("td");
        assertEquals(ga, cells.get(0).text());
        assertEquals(version, cells.get(1).text());
        assertEquals(path, cells.get(2).text());
        assertEquals(size, cells.get(3).text());
    }
}
