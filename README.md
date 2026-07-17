# Gradle Dependency Size Plugin

A plugin to help diagnosing large dependencies, duplicate dependencies.

More diagnostics to follow.

Will resolve all listed dependencies. Useful for pre-caching a large repository for read-only dependency caches, or offline work.

Features:
* Works on Gradle 8.0+
* Works on Java 8+
* Works with configuration-cache and project isolation.
* Contains a "lifecycle" plugin (requires Gradle 8.8+) that will automatically add tasks to all subprojects to export their dependencies.
* Lowest version supported is currently 7.4, as it was the first version that introduced the ArtifactCollection::getResolvedArtifacts api. This support is provided as a "best-effort" and supports the past 4 years worth of Gradle version releases.

Internal details: Similar to jacoco, we need a shared 'output' format that we can write out/read in when loading
dependency report data between projects and test runs. While our configuration stage can cache
top level dependency information, we still should expect to be able to serialize the data using Gradle's own
serialization methods.
Therefore, we rely on protobuf as the wire format for the dependency information. Each project can store a binary blob
encoding its dependency information, and the aggregation task can read these in and dedupe.
