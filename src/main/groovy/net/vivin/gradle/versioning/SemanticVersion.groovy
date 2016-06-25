package net.vivin.gradle.versioning;

/**
 * Created on 6/22/16 at 11:37 AM
 *
 * @author vivin
 */
class SemanticVersion {

    int major;
    int minor;
    int patch;

    SemanticVersion(int major, int minor, int patch) {
        this.major = major
        this.minor = minor
        this.patch = patch
    }

    SemanticVersion bumpMajor() {
        this.major++;
        this.minor = 0;
        this.patch = 0;

        return this;
    }

    SemanticVersion bumpMinor() {
        this.minor++;
        this.patch = 0;

        return this;
    }

    SemanticVersion bumpPatch() {
        this.patch++;

        return this;
    }

    @Override
    String toString() {
        return "${major}.${minor}.${patch}"
    }
}
