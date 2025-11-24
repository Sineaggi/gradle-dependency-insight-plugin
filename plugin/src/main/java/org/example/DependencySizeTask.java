package org.example;

import org.example.internal.DependencyReportWorkAction;
import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.artifacts.component.ModuleComponentIdentifier;
import org.gradle.api.artifacts.component.ProjectComponentIdentifier;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.SetProperty;
import org.gradle.api.tasks.*;
import org.gradle.workers.WorkerExecutor;
import org.jspecify.annotations.NonNull;

import javax.inject.Inject;
import java.io.Serializable;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DependencySizeTask extends DefaultTask {

    sealed interface Reference extends Serializable {
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

    private final WorkerExecutor workerExecutor;

    @Inject
    public DependencySizeTask(WorkerExecutor workerExecutor) {
        this.holders.addAll(ccCompatibleAction());
        this.outputFile.convention(getProject().getLayout().getBuildDirectory().file("reports/dependency-size/data.bin"));
        this.workerExecutor = workerExecutor;
    }

    @TaskAction
    public void run() {
        System.out.println(holders.get());
        System.out.println(holders.get().stream().mapToLong(Holder::size).sum() / 1000.0f / 1000.0f + "MiB");
        //ArrayList<Holder> data = new ArrayList<>(holders.get());
        //data.sort(Comparator.comparing(Holder::path));
        //try (OutputStream os = Files.newOutputStream(outputFile.get().getAsFile().toPath());
        //     ObjectOutputStream oos = new ObjectOutputStream(os)) {
        //    oos.writeObject(data);
        //} catch (IOException e) {
        //    throw new GradleException("failed to write", e);
        //}
        workerExecutor.classLoaderIsolation(f -> {
            f.getClasspath().from(getWorkerClasspath());
        }).submit(DependencyReportWorkAction.class, action -> {
            action.getOutputFile().convention(getOutputFile());
            action.getHolders().set(getHolders());
        });
    }

    private SetProperty<@NonNull Holder> ccCompatibleAction() {
        SetProperty<@NonNull Holder> deps = getProject().getObjects().setProperty(Holder.class);
        Project project = getProject();
        project.getConfigurations().forEach(configuration -> {
            if (!configuration.isCanBeResolved()) {
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
