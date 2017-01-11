package net.vivin.gradle.versioning

import org.gradle.tooling.BuildException

import java.util.regex.Pattern

class SemanticBuildVersionConfiguration {
    String startingVersion = "0.1.0"
    String tagPrefix = ""
    String snapshotSuffix = "SNAPSHOT"
    Pattern tagPattern = ~/\d++\.\d++\.\d++/
    VersionsMatching matching
    PreRelease preRelease
    Autobump autobump = new Autobump()

    public void validate() {
        if(!(startingVersion ==~ /\d++\.\d++\.\d++/)) {
            throw new BuildException("Starting version must be a valid semantic version without identifiers", null)
        }
        tagPrefix = tagPrefix.trim()
        matching?.validate()
        preRelease?.validate()
        autobump?.validate()
    }
}
