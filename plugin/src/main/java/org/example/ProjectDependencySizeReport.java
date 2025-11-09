package org.example;

import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Provider;
import org.gradle.api.provider.SetProperty;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.diagnostics.DependencyReportTask;
import org.gradle.internal.component.external.model.DefaultModuleComponentIdentifier;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.PrintStream;
import java.io.Serializable;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class ProjectDependencySizeReport extends DefaultTask {

    sealed interface Reference extends Serializable {
        public record GAV(String groupId, String artifactId, String version) implements Reference {
        }
        public record StringGav(String gav) implements Reference {
        }
    }

    public record Holder(Reference reference, String path, long size) implements Serializable {
    }

    private final SetProperty<Holder> holders = getProject().getObjects().setProperty(Holder.class);
    private final RegularFileProperty outputFile = getProject().getObjects().fileProperty().convention(getProject().getLayout().getBuildDirectory().file("reports/dependencies.txt"));

    @Input
    public SetProperty<Holder> getHolders() {
        return holders;
    }

    @OutputFile
    public RegularFileProperty getOutputFile() {
        return outputFile;
    }

    public ProjectDependencySizeReport() {
        this.holders.addAll(ccIncompatibleAction2());
    }

    @TaskAction
    public void go() {
        System.out.println(holders.get());
        System.out.println(holders.get().stream().mapToLong(it -> it.size).sum() / 1000.0f / 1000.0f + "MiB");
        try (var os = Files.newOutputStream(outputFile.get().getAsFile().toPath());
             var out = new PrintStream(os)
        ) {
            holders.get()
                    .stream()
                    .map(Holder::path)
                    .sorted()
                    .forEach(out::println);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

    }

    private SetProperty<Holder> ccIncompatibleAction2() {
        //Set<Project> allprojects = getProject().getAllprojects();
        //SetProperty<Holder> deps = getProject().getObjects().setProperty(Holder.class);
        //allprojects.forEach(this::iterateConfigurations);
        return iterateConfigurations(getProject());
    }

    private SetProperty<Holder> iterateConfigurations(Project project) {
        SetProperty<Holder> deps = getProject().getObjects().setProperty(Holder.class);

        project.getConfigurations().forEach(configuration -> {
            if (!configuration.isCanBeResolved()) {
                return;
            }

            Provider<Set<Holder>> configHolder = configuration.getIncoming().getArtifacts().getResolvedArtifacts().map(i -> {
                return i.stream().map(j -> {
                    System.out.println("got j " + j);
                    if (j.getVariant().getOwner() instanceof DefaultModuleComponentIdentifier id) {
                        return new Holder(new Reference.GAV(id.getGroup(), id.getModule(), id.getVersion()), j.getFile().getAbsolutePath(), j.getFile().length());
                    } else {
                        throw new RuntimeException("fuck 2");
                    }
                    //System.out.println(j.getVariant().getOwner().getClass());

                }).collect(Collectors.toSet());
            });

             deps.addAll(configHolder);

            //configuration.getDependencies().stream().filter(it -> {
            //    //System.out.println(it.getClass());
            //    //if (it instanceof ExternalDependency externalDependency) {
            //    //    System.out.println(externalDependency.getGroup());
            //    //    System.out.println(externalDependency.getName());
            //    //    System.out.println(externalDependency.getVersion());
            //    //}
            //    return it instanceof Dependency;
            //}).toList();
            //holderSet.add(new Holder(new GAV("1", "2", "3"), 123));
        });

        return deps;
    }
}
