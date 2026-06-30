package io.github.sineaggi.gradle.dependencysize.internal;

import org.gradle.internal.html.SimpleHtmlWriter;
import org.gradle.reporting.ReportRenderer;
import org.gradle.reporting.TabbedPageRenderer;
import org.gradle.reporting.TabsRenderer;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

class DependencyAggregateReportRenderer extends TabbedPageRenderer<AggregateReportModel> {

    private static final URL STYLE_URL = Objects.requireNonNull(DependencyAggregateReportRenderer.class
            .getResource("/org/example/dependency-size-report.css"));

    private final TabsRenderer<AggregateReportModel> tabsRenderer = new TabsRenderer<>();

    DependencyAggregateReportRenderer() {
        tabsRenderer.add("Size Overview", new SizeOverviewRenderer());
        tabsRenderer.add("Duplicate Versions", new DuplicateVersionsRenderer());
    }

    @Override
    protected String getTitle() {
        return "Dependency Size Report";
    }

    @Override
    protected URL getStyleUrl() {
        return STYLE_URL;
    }

    @Override
    protected ReportRenderer<AggregateReportModel, SimpleHtmlWriter> getHeaderRenderer() {
        return new ReportRenderer<AggregateReportModel, SimpleHtmlWriter>() {
            @Override
            public void render(AggregateReportModel model, SimpleHtmlWriter html) throws IOException {
                html.startElement("div").attribute("class", "stat-bar");
                statBox(html, "Total Dependencies", String.valueOf(model.totalDeps));
                statBox(html, "Total Size", DependencyReportAggregationWorkAction.humanReadableByteCountBin(model.totalSize));
                statBox(html, "Duplicate GAs", String.valueOf(model.duplicates.size()));
                html.endElement();
            }
        };
    }

    @Override
    protected ReportRenderer<AggregateReportModel, SimpleHtmlWriter> getContentRenderer() {
        return tabsRenderer;
    }

    private static void statBox(SimpleHtmlWriter html, String label, String value) throws IOException {
        html.startElement("div").attribute("class", "stat-box")
            .startElement("div").attribute("class", "stat-value").characters(value).endElement()
            .startElement("div").attribute("class", "stat-label").characters(label).endElement()
            .endElement();
    }

    private static class SizeOverviewRenderer extends ReportRenderer<AggregateReportModel, SimpleHtmlWriter> {
        @Override
        public void render(AggregateReportModel model, SimpleHtmlWriter html) throws IOException {
            html.startElement("h3").characters("Top 10 Largest").endElement();
            renderSizeTable(html, model.largest);
            html.startElement("h3").characters("Top 10 Smallest").endElement();
            renderSizeTable(html, model.smallest);
        }

        private static void renderSizeTable(SimpleHtmlWriter html, List<SimpleDep> deps) throws IOException {
            html.startElement("table");
            html.startElement("thead");
            html.startElement("tr");
            for (String col : new String[]{"Dependency", "Version", "Subproject", "Size"}) {
                html.startElement("th").characters(col).endElement();
            }
            html.endElement(); // tr
            html.endElement(); // thead
            html.startElement("tbody");
            for (SimpleDep dep : deps) {
                html.startElement("tr");
                html.startElement("td").characters(dep.ga()).endElement();
                html.startElement("td").characters(dep.version()).endElement();
                html.startElement("td").characters(dep.path()).endElement();
                html.startElement("td").attribute("class", "num").characters(DependencyReportAggregationWorkAction.humanReadableByteCountBin(dep.size())).endElement();
                html.endElement(); // tr
            }
            html.endElement(); // tbody
            html.endElement(); // table
        }
    }

    private static class DuplicateVersionsRenderer extends ReportRenderer<AggregateReportModel, SimpleHtmlWriter> {
        @Override
        public void render(AggregateReportModel model, SimpleHtmlWriter html) throws IOException {
            if (model.duplicates.isEmpty()) {
                html.startElement("p").characters("No duplicate dependencies found.").endElement();
                return;
            }
            html.startElement("table");
            html.startElement("thead");
            html.startElement("tr");
            for (String col : new String[]{"Dependency", "Total Size", "Count", "Subprojects & Versions"}) {
                html.startElement("th").characters(col).endElement();
            }
            html.endElement(); // tr
            html.endElement(); // thead
            html.startElement("tbody");
            for (Map.Entry<String, List<SimpleDep>> entry : model.duplicates) {
                List<SimpleDep> deps = entry.getValue();
                long totalSize = deps.stream().mapToLong(SimpleDep::size).sum();
                String subprojects = deps.stream()
                        .map(d -> d.path() + " (" + d.version() + ")")
                        .collect(Collectors.joining(", "));
                html.startElement("tr");
                html.startElement("td").characters(entry.getKey()).endElement();
                html.startElement("td").attribute("class", "num").characters(DependencyReportAggregationWorkAction.humanReadableByteCountBin(totalSize)).endElement();
                html.startElement("td").attribute("class", "num").characters(String.valueOf(deps.size())).endElement();
                html.startElement("td").characters(subprojects).endElement();
                html.endElement(); // tr
            }
            html.endElement(); // tbody
            html.endElement(); // table
        }
    }
}
