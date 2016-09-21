package net.vivin.gradle.versioning

import org.gradle.tooling.BuildException

import java.util.regex.Pattern

class VersionsMatching {
    int major = -1
    int minor = -1
    int patch = -1

    void validate() {
        if(major < 0 && minor < 0 && patch < 0) {
            throw new BuildException("Matching versions must be specified", null)
        }

        if((major < 0 && minor >= 0) || (minor < 0 && patch >= 0)) {
            throw new BuildException("When specifying a matching versioning-component, all preceding components (if any) must also be specified", null)
        }
    }

    Pattern toPattern() {
        validate()

        Pattern pattern = ~/${major}\./

        if(minor >= 0) {
            pattern = ~/${pattern}${minor}\./

            if(patch >= 0) {
                pattern = ~/${pattern}${patch}/
            }
        }

        pattern
    }
}
