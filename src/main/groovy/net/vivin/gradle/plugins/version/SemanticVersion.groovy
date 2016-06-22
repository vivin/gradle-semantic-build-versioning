package net.vivin.gradle.plugins.version;

class SemanticVersion {

    int major;
    int minor;
    int patch;

    SemanticVersion(int major, int minor, int patch) {
        this.major = major
        this.minor = minor
        this.patch = patch
    }

    SemanticVersion(String version) {
        if(!(version ==~ /^\d+\.\d+\.\d+$/)) {
            throw new IllegalArgumentException("${version} is not a valid semantic-version");
        }

        String[] components = version.split(/\./)
        this.major = Integer.parseInt(components[0]);
        this.minor = Integer.parseInt(components[1]);
        this.patch = Integer.parseInt(components[2]);
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
