package org.example;

import org.example.internal.DefaultDependencySizeReport;
import org.example.internal.SerializableLambdas;
import org.gradle.api.NamedDomainObjectProvider;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.artifacts.DependencyScopeConfiguration;
import org.gradle.api.artifacts.ResolvableConfiguration;
import org.gradle.api.artifacts.component.ComponentIdentifier;
import org.gradle.api.artifacts.component.ProjectComponentIdentifier;
import org.gradle.api.artifacts.type.ArtifactTypeDefinition;
import org.gradle.api.file.FileCollection;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.plugins.JavaBasePlugin;
import org.gradle.api.plugins.jvm.internal.JvmPluginServices;
import org.gradle.api.provider.Provider;
import org.gradle.api.reporting.ReportingExtension;
import org.gradle.api.specs.Spec;
import org.jspecify.annotations.NonNull;

import javax.inject.Inject;

public abstract class DependencySizeReportAggregationPlugin implements Plugin<Project> {

    public static final String DEPENDENCY_SIZE_AGGREGATION_CONFIGURATION_NAME = "dependencySizeAggregation";

    @Inject
    protected abstract JvmPluginServices getEcosystemUtilities();

    @Override
    public void apply(Project project) {
        // todo: report aggregation
        project.getPluginManager().apply("org.gradle.reporting-base");
        project.getPluginManager().apply("jvm-ecosystem");
        project.getPluginManager().apply("dependency-size-report");

        project.getPlugins().apply(DependencySizeBasePlugin.class);

        ObjectFactory objects = project.getObjects();
        ConfigurationContainer configurations = project.getConfigurations();
        NamedDomainObjectProvider<@NonNull DependencyScopeConfiguration> dependencySizeAggregation = configurations.dependencyScope(DEPENDENCY_SIZE_AGGREGATION_CONFIGURATION_NAME, conf -> {
            conf.setDescription("Collects project dependencies for purposes of dependency size report aggregation");
        });

        NamedDomainObjectProvider<@NonNull ResolvableConfiguration> dependencySizeResultsConf = configurations.resolvable("aggregateCodeCoverageReportResults", conf -> {
            conf.setDescription("Resolvable configuration used to gather files for the dependency size report aggregation via ArtifactViews, not intended to be used directly");
            conf.extendsFrom(dependencySizeAggregation.get());
            project.getPlugins().withType(JavaBasePlugin.class, plugin -> {
                // If the current project is jvm-based, aggregate dependent projects as jvm-based as well.
                getEcosystemUtilities().configureAsRuntimeClasspath(conf);
            });
        });

        ReportingExtension reporting = project.getExtensions().getByType(ReportingExtension.class);
        System.out.println("reportyor");
        reporting.getReports().registerBinding(DependencySizeReport.class, DefaultDependencySizeReport.class);
        reporting.getReports().withType(DependencySizeReport.class).all((report) -> report.getReportTask().configure((task) -> {
            Provider<@NonNull FileCollection> executionData = dependencySizeResultsConf.map((conf) -> conf.getIncoming().artifactView((view) -> {
                view.withVariantReselection();
                view.componentFilter(projectComponent());
                view.attributes((attributes) -> {
                    //attributes.attribute(Category.CATEGORY_ATTRIBUTE, (Category)objects.named(Category.class, "verification"));
                    //attributes.attributeProvider(TestSuiteName.TEST_SUITE_NAME_ATTRIBUTE, report.getTestSuiteName().map((tt) -> (TestSuiteName)objects.named(TestSuiteName.class, tt)));
                    //attributes.attribute(VerificationType.VERIFICATION_TYPE_ATTRIBUTE, (VerificationType)objects.named(VerificationType.class, "jacoco-coverage"));
                    attributes.attribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE, "binary");
                });
            }).getFiles());

            configureReportTaskInputs(task, executionData);

            task.getWorkerClasspath().from(project.getConfigurations().named(DependencySizeBasePlugin.DEPENDENCY_SIZE_TOOLING));
        }));
        reporting.getReports().register("dependencySizeReport", DependencySizeReport.class, (report) -> {
        });
    }

    private static Spec<ComponentIdentifier> projectComponent() {
        return SerializableLambdas.spec((id) -> id instanceof ProjectComponentIdentifier);
    }

    private void configureReportTaskInputs(DependencySizeAggregationTask task, Provider<@NonNull FileCollection> executionData) {
        task.getOthers().from(executionData);
    }
}
