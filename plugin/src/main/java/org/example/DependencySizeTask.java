package org.example;

import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.artifacts.component.ModuleComponentIdentifier;
import org.gradle.api.artifacts.component.ProjectComponentIdentifier;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.SetProperty;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;
import org.jspecify.annotations.NonNull;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Comparator;
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

    @Input
    public SetProperty<@NonNull Holder> getHolders() {
        return holders;
    }

    @OutputFile
    public RegularFileProperty getOutputFile() {
        return outputFile;
    }

    public DependencySizeTask() {
        this.holders.addAll(ccCompatibleAction());
        this.outputFile.convention(getProject().getLayout().getBuildDirectory().file("reports/dependency-size/data.bin"));
    }

    @TaskAction
    public void run() {
        System.out.println(holders.get());
        System.out.println(holders.get().stream().mapToLong(Holder::size).sum() / 1000.0f / 1000.0f + "MiB");
        ArrayList<Holder> data = new ArrayList<>(holders.get());
        data.sort(Comparator.comparing(Holder::path));
        try (OutputStream os = Files.newOutputStream(outputFile.get().getAsFile().toPath());
             ObjectOutputStream oos = new ObjectOutputStream(os)) {
            oos.writeObject(data);
        } catch (IOException e) {
            throw new GradleException("failed to write", e);
        }
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
