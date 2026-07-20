package io.github.sineaggi.gradle.dependencysize.internal;

final class HolderData {
    private final String configurationName;
    private final String path;
    private final long size;
    private final String gav;
    private final String group;
    private final String artifact;
    private final String version;

    HolderData(String configurationName, String path, long size, String gav, String group, String artifact, String version) {
        this.configurationName = configurationName;
        this.path = path;
        this.size = size;
        this.gav = gav;
        this.group = group;
        this.artifact = artifact;
        this.version = version;
    }

    String getConfigurationName() { return configurationName; }
    String getPath() { return path; }
    long getSize() { return size; }
    String getGav() { return gav; }
    String getGroup() { return group; }
    String getArtifact() { return artifact; }
    String getVersion() { return version; }
}
