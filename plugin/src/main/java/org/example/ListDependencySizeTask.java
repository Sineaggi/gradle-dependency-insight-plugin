package org.example;

import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.component.ModuleComponentIdentifier;
import org.gradle.api.artifacts.component.ProjectComponentIdentifier;
import org.gradle.api.provider.SetProperty;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.TaskAction;
import org.jspecify.annotations.NonNull;

import java.io.Serializable;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ListDependencySizeTask extends DefaultTask {

    sealed interface Reference extends Serializable {
        public record GAV(String groupId, String artifactId, String version) implements Reference {
        }

        public record StringGav(String gav) implements Reference {
        }

        public record ProjectPath(String projectName, String projectPath) implements Reference {
        }
    }

    public record Holder(Reference reference, String path, long size) implements Serializable {
    }

    private final SetProperty<Holder> holders = getProject().getObjects().setProperty(Holder.class);
    private final SetProperty<Dependency> dependencies = getProject().getObjects().setProperty(Dependency.class);

    @Input
    public SetProperty<Holder> getHolders() {
        return holders;
    }

    public ListDependencySizeTask() {
        this.holders.addAll(ccCompatibleAction());
    }

    @TaskAction
    public void run() {
        System.out.println(holders.get());
        System.out.println(holders.get().stream().mapToLong(it -> it.size).sum() / 1000.0f / 1000.0f + "MiB");
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
                    return Stream.of(new Holder(new Reference.GAV(id.getGroup(), id.getModule(), id.getVersion()), artifactResult.getFile().getAbsolutePath(), artifactResult.getFile().length()));
                } else if (artifactResult.getVariant().getOwner() instanceof ProjectComponentIdentifier id) {
                    // todo: this should no longer be possible.
                    return Stream.of(new Holder(new Reference.ProjectPath(id.getProjectName(), id.getProjectPath()), artifactResult.getFile().getAbsolutePath(), artifactResult.getFile().length()));
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
