package net.vivin.gradle.versioning

import org.gradle.tooling.BuildException

import java.util.regex.Pattern

class PreRelease {
    static final String PRE_RELEASE_PART_REGEX = /(?!-)(?:(?:[1-9]\d*+|\p{Alpha}|[\p{Alnum}-]*[\p{Alpha}-][\p{Alnum}-]*+)(?<!-)(?:\.(?![$-])|$)?+)++/

    String startingVersion
    String pattern = /.*+$/
    Closure<?> bump

    void validate() {
        if(!startingVersion) {
            throw new BuildException("A startingVersion must be specified in preRelease", null)
        }

        if(!pattern) {
            throw new BuildException("A valid pattern must be specified in preRelease", null)
        }

        if(!bump) {
            throw new BuildException("Bumping scheme for preRelease versions must be specified", null)
        }

        if(isValidPreReleasePart(startingVersion)) {
            return
        }

        String[] components = startingVersion.split(/\./)
        components.each { component ->
            if(!(component ==~ /[\p{Alnum}-]++/)) {
                throw new BuildException("Identifiers must comprise only ASCII alphanumerics and hyphen", null)
            }

            if(component ==~ /0\d++/) {
                throw new BuildException("Numeric identifiers must not include leading zeroes", null)
            }
        }
    }

    Pattern getPattern() {
        ~/\d++\.\d++\.\d++-$pattern/
    }

    static boolean isValidPreReleasePart(String preReleasePart) {
        preReleasePart ==~ PRE_RELEASE_PART_REGEX
    }
}
