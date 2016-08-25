package net.vivin.gradle.versioning

import org.gradle.tooling.BuildException

import java.util.regex.Pattern

class Autobump {
    Pattern majorPattern = ~/^\[major\]$/
    Pattern minorPattern = ~/^\[minor\]$/
    Pattern patchPattern = ~/^\[patch\]$/
    Pattern preReleasePattern = ~/^\[pre-release\]$/
    Pattern newPreReleasePattern = ~/^\[new-pre-release\]$/
    Pattern promoteToReleasePattern = ~/^\[promote\]$/

    void validate() {
        if(!majorPattern) {
            throw new BuildException("Valid majorPattern must be specified in autobump", null)
        }

        if(!minorPattern) {
            throw new BuildException("Valid minorPattern must be specified in autobump", null)
        }

        if(!patchPattern) {
            throw new BuildException("Valid patchPattern must be specified in autobump", null)
        }

        if(!preReleasePattern) {
            throw new BuildException("Valid preReleasePattern must be specified in autobump", null)
        }

        if(!newPreReleasePattern) {
            throw new BuildException("Valid newPreReleasePattern must be specified in autobump", null)
        }

        if(!promoteToReleasePattern) {
            throw new BuildException("Valid promoteToReleasePattern must be specified in autobump", null)
        }
    }
}