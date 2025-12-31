# Gradle Dependency Size Plugin

A plugin to help diagnosing large dependencies, duplicate dependencies.

More diagnostics to follow.

Will resolve all listed dependencies. Useful for pre-caching a large repository for read-only dependency caches, or offline work.

Features:
* Works on Gradle 8.0+
* Works on Java 8+
* Works with configuration-cache and project isolation.
* Contains a "lifecycle" plugin (requires Gradle 8.8+) that will automatically add tasks to all subprojects to export their dependencies.
