package io.github.sineaggi.gradle.dependencysize;

import io.github.sineaggi.gradle.dependencysize.internal.DefaultDependencySizeReport;
import io.github.sineaggi.gradle.dependencysize.internal.SerializableLambdas;
import io.github.sineaggi.gradle.dependencysize.tasks.DependencySizeAggregationTask;
import org.gradle.api.NamedDomainObjectProvider;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.artifacts.DependencyScopeConfiguration;
import org.gradle.api.artifacts.ResolvableConfiguration;
import org.gradle.api.artifacts.component.ComponentIdentifier;
import org.gradle.api.artifacts.component.ProjectComponentIdentifier;
import org.gradle.api.artifacts.type.ArtifactTypeDefinition;
import org.gradle.api.attributes.Category;
import org.gradle.api.file.FileCollection;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Provider;
import org.gradle.api.reporting.ReportingExtension;
import org.gradle.api.specs.Spec;

@SuppressWarnings("unused")
public abstract class DependencySizeReportAggregationPlugin implements Plugin<Project> {

    public static final String DEPENDENCY_SIZE_AGGREGATION_CONFIGURATION_NAME = "dependencySizeAggregation";

    @Override
    public void apply(Project project) {
        project.getPluginManager().apply("org.gradle.reporting-base");
        project.getPluginManager().apply("io.github.sineaggi.dependency-size-report");

        ObjectFactory objects = project.getObjects();
        ConfigurationContainer configurations = project.getConfigurations();
        NamedDomainObjectProvider<DependencyScopeConfiguration> dependencySizeAggregation = configurations.dependencyScope(DEPENDENCY_SIZE_AGGREGATION_CONFIGURATION_NAME, conf -> {
            conf.setDescription("Collects project dependencies for purposes of dependency size report aggregation");
        });

        NamedDomainObjectProvider<ResolvableConfiguration> dependencySizeResultsConf = configurations.resolvable("aggregateDependencySizeReportResults", conf -> {
            conf.setDescription("Resolvable configuration used to gather files for the dependency size report aggregation via ArtifactViews, not intended to be used directly");
            conf.extendsFrom(dependencySizeAggregation.get());
            conf.attributes(attributes -> {
                attributes.attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.class, DependencySizeReportPlugin.DEPENDENCY_SIZE_CATEGORY));
            });
        });

        ReportingExtension reporting = project.getExtensions().getByType(ReportingExtension.class);
        reporting.getReports().registerBinding(DependencySizeReport.class, DefaultDependencySizeReport.class);
        reporting.getReports().withType(DependencySizeReport.class).all((report) -> report.getReportTask().configure((task) -> {
            Provider<FileCollection> executionData = dependencySizeResultsConf.map((conf) -> conf.getIncoming().artifactView((view) -> {
                view.withVariantReselection();
                view.lenient(true);
                view.componentFilter(projectComponent());
                view.attributes((attributes) -> {
                    attributes.attribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE, "binary");
                });
            }).getFiles());

            configureReportTaskInputs(task, executionData);

            task.getOutputFile().convention(
                    reporting.getBaseDirectory().file("dependency-size/" + report.getName() + ".html"));
        }));
        reporting.getReports().register("dependencySizeReport", DependencySizeReport.class, (report) -> {
        });
    }

    private static Spec<ComponentIdentifier> projectComponent() {
        return SerializableLambdas.spec((id) -> id instanceof ProjectComponentIdentifier);
    }

    private void configureReportTaskInputs(DependencySizeAggregationTask task, Provider<FileCollection> executionData) {
        task.getOthers().from(executionData);
    }
}
