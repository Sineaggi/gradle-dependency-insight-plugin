package org.example;

import org.gradle.api.DefaultTask;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.ExternalDependency;
import org.gradle.api.artifacts.component.ComponentIdentifier;
import org.gradle.api.provider.SetProperty;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.TaskAction;
import org.gradle.internal.component.external.model.DefaultModuleComponentIdentifier;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class ListDependencySizeTask extends DefaultTask {

    sealed interface Reference extends Serializable {
        public record GAV(String groupId, String artifactId, String version) implements Reference {
        }
        public record StringGav(String gav) implements Reference {
        }
    }
    //public record StringPath(String path) implements Serializable {
    //}

    public record Holder(Reference reference, String path, long size) implements Serializable {
    }

    private final SetProperty<Holder> holders = getProject().getObjects().setProperty(Holder.class);
    private final SetProperty<Dependency> dependencies = getProject().getObjects().setProperty(Dependency.class);

    @Input
    public SetProperty<Holder> getHolders() {
        return holders;
    }

    @Input
    public SetProperty<Dependency> getDependencies() {
        return dependencies;
    }

    public ListDependencySizeTask() {
        //this.holders.convention(
        //        getProject().getProviders().provider(this::ccIncompatibleAction));
        this.holders.addAll(ccIncompatibleAction2());
        //this.configurations.add(getProject().getConfigurations().named("implementation"));
    }

    @TaskAction
    public void go() {
        //fun ccIncompatibleAction(): Set<PublishProblem> {
        //    val allprojects = project.allprojects
        //    allprojects.forEach(Consumer { project: Project ->
        //            project.configurations.forEach(Consumer { configuration: Configuration ->
//
        //    })
        //    })
        //}
        System.out.println(holders.get());
        System.out.println(holders.get().stream().mapToLong(it -> it.size).sum() / 1000.0f / 1000.0f + "MiB");
    }

    private Set<Holder> ccIncompatibleAction() {
        var allprojects = getProject().getAllprojects();
        Set<Holder> holderSet = new HashSet<>();
        allprojects.forEach(project -> {
            project.getConfigurations().forEach(configuration -> {
            /*
            val externalDependencies =
        configuration.dependencies.stream().filter { it: Dependency -> it !is ProjectDependency }
    externalDependencies.forEach { externalDependency: Dependency ->
        val group = externalDependency.group
        if (group == null) {
            logger.warn(
                "Dependency {} in configuration {} of project {} has no group set, skipping",
                externalDependency.name,
                configuration.name,
                project.name
            )
            return@forEach
        }
        val ga: GA = GA(group, externalDependency.name)
        val projectInfo = gas[ga]
        if (projectInfo != null) {
            publishProblems.add(
                PublishProblem(
                    ProjectInfo(project.name, project.path), projectInfo, ga
                )
            )
        }
             */
                //configuration.getResolvedConfiguration()
                //                .getResolvedArtifacts()
                //
                if (!configuration.isCanBeResolved()) {
                    return;
                }
                configuration.getIncoming().getDependencies();
                //configuration.getIncoming().getDependencies().stream()
                //        .filter(it -> {
                //            System.out.println(it.getClass());
                //            if (it instanceof ExternalDependency externalDependency) {
                //                System.out.println(externalDependency.getGroup());
                //                System.out.println(externalDependency.getName());
                //                System.out.println(externalDependency.getVersion());
                //            }
                //            return true;
                //        }).toList();
                //configuration.getResolvedConfiguration()
                //                .getResolvedArtifacts()
                //                        .stream()
                //                                .filter(i -> {
                //                                    ResolvedArtifact it;
                //                                    return true;
                //                                }).toList();
                configuration.getDependencies().stream().filter(it -> {
                    //System.out.println(it.getClass());
                    //if (it instanceof ExternalDependency externalDependency) {
                    //    System.out.println(externalDependency.getGroup());
                    //    System.out.println(externalDependency.getName());
                    //    System.out.println(externalDependency.getVersion());
                    //}
                    return it instanceof Dependency;
                }).toList();
                //holderSet.add(new Holder(new Reference.GAV("1", "2", "3"), 123));
            });
        });
        return holderSet;
    }

    private SetProperty<Holder> ccIncompatibleAction2() {
        var allprojects = getProject().getAllprojects();
        var deps = getProject().getObjects().setProperty(Holder.class);
        //Set<Dependency> holderSet = new HashSet<>();
        allprojects.forEach(project -> {
            project.getConfigurations().forEach(configuration -> {
            /*
            val externalDependencies =
        configuration.dependencies.stream().filter { it: Dependency -> it !is ProjectDependency }
    externalDependencies.forEach { externalDependency: Dependency ->
        val group = externalDependency.group
        if (group == null) {
            logger.warn(
                "Dependency {} in configuration {} of project {} has no group set, skipping",
                externalDependency.name,
                configuration.name,
                project.name
            )
            return@forEach
        }
        val ga: GA = GA(group, externalDependency.name)
        val projectInfo = gas[ga]
        if (projectInfo != null) {
            publishProblems.add(
                PublishProblem(
                    ProjectInfo(project.name, project.path), projectInfo, ga
                )
            )
        }
             */
                //configuration.getResolvedConfiguration()
                //                .getResolvedArtifacts()
                //
                if (!configuration.isCanBeResolved()) {
                    return;
                }

                var fo = configuration.getIncoming().getArtifacts().getResolvedArtifacts().map(i -> {
                    return i.stream().map(j -> {
                        //System.out.println(j.getClass());
                        //System.out.println(j.getVariant());
                        //System.out.println(j.getVariant().getOwner());
                        if (j.getVariant().getOwner() instanceof DefaultModuleComponentIdentifier id) {
                            //System.out.println(id.getGroup());
                            //System.out.println(id.getModule());
                            //System.out.println(id.getVersion());
                            System.out.println(id.toString());
                        } else {
                            throw new RuntimeException("fuck");
                        }
                        //System.out.println(j.getVariant().getOwner().getClass());
                        return new Holder(new Reference.GAV(id.getGroup(), id.getModule(), id.getVersion()), j.getFile().getAbsolutePath(), j.getFile().length());
                    }).collect(Collectors.toSet());
                });

                //configuration.getIncoming().getDependencies().stream().map(i -> {
                //    if (i instanceof ExternalDependency externalDependency) {
                //
                //    }
                //})
                deps.addAll(fo);

                //deps.addAll(configuration.getIncoming().getDependencies());
                //configuration.getIncoming().getDependencies().stream()
                //        .filter(it -> {
                //            System.out.println(it.getClass());
                //            if (it instanceof ExternalDependency externalDependency) {
                //                System.out.println(externalDependency.getGroup());
                //                System.out.println(externalDependency.getName());
                //                System.out.println(externalDependency.getVersion());
                //            }
                //            return true;
                //        }).toList();
                //configuration.getResolvedConfiguration()
                //                .getResolvedArtifacts()
                //                        .stream()
                //                                .filter(i -> {
                //                                    ResolvedArtifact it;
                //                                    return true;
                //                                }).toList();
                configuration.getDependencies().stream().filter(it -> {
                    //System.out.println(it.getClass());
                    //if (it instanceof ExternalDependency externalDependency) {
                    //    System.out.println(externalDependency.getGroup());
                    //    System.out.println(externalDependency.getName());
                    //    System.out.println(externalDependency.getVersion());
                    //}
                    return it instanceof Dependency;
                }).toList();
                //holderSet.add(new Holder(new GAV("1", "2", "3"), 123));
            });
        });
        return deps;
    }
}
