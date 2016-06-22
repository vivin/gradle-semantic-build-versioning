package net.vivin.gradle.plugins.version.git;

public enum VersionComponent {
    NONE(-1), MAJOR(0), MINOR(1), PATCH(2), PRERELEASE(3);

    private int index;

    VersionComponent(int index) {
        this.index = index;
    }

    public int getIndex() {
        return index;
    }
}
