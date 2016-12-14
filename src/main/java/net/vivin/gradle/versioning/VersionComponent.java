package net.vivin.gradle.versioning;

public enum VersionComponent {
    NONE(-1), PRERELEASE(3), PATCH(2), MINOR(1), MAJOR(0);

    private int index;

    VersionComponent(int index) {
        this.index = index;
    }

    public int getIndex() {
        return index;
    }
}
