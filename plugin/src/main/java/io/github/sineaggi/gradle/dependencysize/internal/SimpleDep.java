package io.github.sineaggi.gradle.dependencysize.internal;

import java.util.Objects;

final class SimpleDep {
    private final String path;
    private final long size;
    private final String ga;
    private final String version;

    SimpleDep(String path, long size, String ga, String version) {
        this.path = path;
        this.size = size;
        this.ga = ga;
        this.version = version;
    }

    String path() { return path; }
    long size() { return size; }
    String ga() { return ga; }
    String version() { return version; }
    String gav() { return ga + ":" + version; }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        SimpleDep that = (SimpleDep) obj;
        return Objects.equals(this.path, that.path) &&
               this.size == that.size &&
               Objects.equals(this.ga, that.ga) &&
               Objects.equals(this.version, that.version);
    }

    @Override
    public int hashCode() {
        return Objects.hash(path, size, ga, version);
    }
}
