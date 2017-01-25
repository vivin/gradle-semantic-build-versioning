package net.vivin.gradle.versioning

import java.util.regex.Pattern

class Autobump {
    Pattern majorPattern = ~/\[major\]/
    Pattern minorPattern = ~/\[minor\]/
    Pattern patchPattern = ~/\[patch\]/
    Pattern newPreReleasePattern = ~/\[new-pre-release\]/
    Pattern promoteToReleasePattern = ~/\[promote\]/

    public boolean isEnabled() {
        majorPattern || minorPattern || patchPattern || newPreReleasePattern || promoteToReleasePattern
    }
}
