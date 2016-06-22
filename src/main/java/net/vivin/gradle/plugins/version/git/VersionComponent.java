package net.vivin.gradle.plugins.version.git;

/**
 * Created on 2/10/16 at 12:04 AM
 *
 * @author vivin
 */
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
