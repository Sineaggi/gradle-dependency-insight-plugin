package org.example.tasks;

import org.example.internal.DependencyReportWorkAction;
import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.artifacts.component.ComponentIdentifier;
import org.gradle.api.artifacts.component.ModuleComponentIdentifier;
import org.gradle.api.artifacts.component.ProjectComponentIdentifier;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.ProjectLayout;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.model.ObjectFactory;
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

    public interface Reference {
        interface GAV extends Reference {
            Property<String> getGroupId();
            Property<String> getArtifactId();
            Property<String> getVersion();
        }

        interface ProjectPath extends Reference {
            Property<String> getProjectName();
            Property<String> getProjectPath();
        }
    }

    public interface Holder {
        Property<String> getConfigurationName();
        Property<Reference> getReference();
        Property<String> getPath();
        Property<Long> getSize();
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
        ObjectFactory objects = project.getObjects();
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
                Holder holder = objects.newInstance(Holder.class);
                holder.getConfigurationName().set(configuration.getName());
                holder.getPath().set(artifactResult.getFile().getAbsolutePath());
                holder.getSize().set(artifactResult.getFile().length());
                ComponentIdentifier owner = artifactResult.getVariant().getOwner();
                if (owner instanceof ModuleComponentIdentifier) {
                    ModuleComponentIdentifier id = (ModuleComponentIdentifier) owner;
                    Reference.GAV reference = objects.newInstance(Reference.GAV.class);
                    reference.getGroupId().set(id.getGroup());
                    reference.getArtifactId().set(id.getModule());
                    reference.getVersion().set(id.getVersion());
                    holder.getReference().set(reference);
                    return Stream.of(holder);
                } else if (owner instanceof ProjectComponentIdentifier) {
                    ProjectComponentIdentifier id = (ProjectComponentIdentifier) owner;
                    // todo: this should no longer be possible.
                    Reference.ProjectPath reference = objects.newInstance(Reference.ProjectPath.class);
                    reference.getProjectName().set(id.getProjectName());
                    reference.getProjectPath().set(id.getProjectPath());
                    holder.getReference().set(reference);
                    return Stream.of(holder);
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
