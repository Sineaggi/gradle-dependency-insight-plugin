package org.example.tasks;

import org.example.internal.DependencyReportWorkAction;
import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.artifacts.component.ModuleComponentIdentifier;
import org.gradle.api.artifacts.component.ProjectComponentIdentifier;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.ProjectLayout;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.ProviderFactory;
import org.gradle.api.provider.SetProperty;
import org.gradle.api.tasks.*;
import org.gradle.workers.WorkerExecutor;
import org.jspecify.annotations.NonNull;

import javax.inject.Inject;
import java.io.Serializable;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DependencySizeTask extends DefaultTask {

    public sealed interface Reference extends Serializable {
        public record GAV(String groupId, String artifactId, String version) implements Reference {
        }

        public record StringGav(String gav) implements Reference {
        }

        public record ProjectPath(String projectName, String projectPath) implements Reference {
        }
    }

    public record Holder(String configurationName, Reference reference, String path,
                         long size) implements Serializable {
    }

    private final SetProperty<@NonNull Holder> holders = getProject().getObjects().setProperty(Holder.class);
    private final RegularFileProperty outputFile = getProject().getObjects().fileProperty();
    private final ConfigurableFileCollection workerClasspath = getProject().getObjects().fileCollection();
    private final Property<String> projectId = getProject().getObjects().property(String.class);

    @Input
    public SetProperty<@NonNull Holder> getHolders() {
        return holders;
    }

    @OutputFile
    public RegularFileProperty getOutputFile() {
        return outputFile;
    }

    @Classpath
    public ConfigurableFileCollection getWorkerClasspath() {
        return workerClasspath;
    }

    @Input
    public Property<String> getProjectId() {
        return projectId;
    };

    private final WorkerExecutor workerExecutor;

    @Inject
    public DependencySizeTask(WorkerExecutor workerExecutor, ProviderFactory providers, ProjectLayout layout) {
        this.holders.convention(providers.provider(() -> ccCompatibleAction().get()));
        this.outputFile.convention(layout.getBuildDirectory().file("reports/dependency-size/data.bin"));
        this.workerExecutor = workerExecutor;
        this.projectId.convention(getProject().getPath());
    }

    @TaskAction
    public void run() {
        workerExecutor.classLoaderIsolation(f -> {
            f.getClasspath().from(getWorkerClasspath());
        }).submit(DependencyReportWorkAction.class, action -> {
            action.getOutputFile().convention(getOutputFile());
            action.getHolders().set(getHolders());
            action.getProjectPath().convention(getProjectId());
        });
    }

    private SetProperty<@NonNull Holder> ccCompatibleAction() {
        SetProperty<@NonNull Holder> deps = getProject().getObjects().setProperty(Holder.class);
        Project project = getProject();
        Set<String> configurations = Set.of(
                "compileClasspath",
                "testCompileClasspath",
                "runtimeClasspath",
                "testRuntimeClasspath"
        );
        project.getConfigurations().forEach(configuration -> {
            if (!configuration.isCanBeResolved()) {
                return;
            }
            if (!configurations.contains(configuration.getName())) {
                return;
            }

            deps.addAll(configuration.getIncoming().artifactView(viewConfiguration -> {
                viewConfiguration.componentFilter(componentIdentifier -> {
                    // filter out jars built by project.
                    // required to prevent `getArtifacts().getResolvedArtifacts()` from requiring project to build.
                    // note this will also prevent project jars from being included in final size calculation.
                    return componentIdentifier instanceof ModuleComponentIdentifier;
                });
            }).getArtifacts().getResolvedArtifacts().map(artifactResults -> artifactResults.stream().flatMap(artifactResult -> {
                if (artifactResult.getVariant().getOwner() instanceof ModuleComponentIdentifier id) {
                    return Stream.of(new Holder(configuration.getName(), new Reference.GAV(id.getGroup(), id.getModule(), id.getVersion()), artifactResult.getFile().getAbsolutePath(), artifactResult.getFile().length()));
                } else if (artifactResult.getVariant().getOwner() instanceof ProjectComponentIdentifier id) {
                    // todo: this should no longer be possible.
                    return Stream.of(new Holder(configuration.getName(), new Reference.ProjectPath(id.getProjectName(), id.getProjectPath()), artifactResult.getFile().getAbsolutePath(), artifactResult.getFile().length()));
                } else {
                    // todo: use project warnings to let devs know things haven't gone to plan
                    // throw new GradleException("Unknown project id " + j.getVariant().getOwner().getClass());
                    return Stream.empty();
                }
            }).collect(Collectors.toSet())));
        });
        return deps;
    }
}
