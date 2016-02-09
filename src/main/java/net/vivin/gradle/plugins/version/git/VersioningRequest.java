package net.vivin.gradle.plugins.version.git;

/**
 * Created on 2/8/16 at 9:24 PM
 *
 * @author vivin
 */
public class VersioningRequest {
    private final Bump bump;

    private String identifiers = "";

    private String snapshotSuffix = "SNAPSHOT";
    private String releaseSuffix = "RELEASE";

    private boolean snapshot = false;

    private String major = "";
    private String minor = "";
    private String patch = "";


    public VersioningRequest(Bump bump) {
        this.bump = bump;
    }

    public VersioningRequest(String major, Bump bump) {
        this.major = major;

        this.bump = bump;
    }

    public VersioningRequest(String major, String minor, Bump bump) {
        this.major = major;
        this.minor = minor;

        this.bump = bump;
    }

    public VersioningRequest(String major, String minor, String patch, Bump bump) {
        this.major = major;
        this.minor = minor;
        this.patch = patch;

        this.bump = bump;
    }

    public VersioningRequest withIdentifiers(String identifiers) {
        this.identifiers = identifiers;
        return this;
    }

    public VersioningRequest withSnapshotSuffix(String snapshotSuffix) {
        this.snapshotSuffix = snapshotSuffix;
        return this;
    }

    public VersioningRequest withReleaseSuffix(String releaseSuffix) {
        this.releaseSuffix = releaseSuffix;
        return this;
    }

    public VersioningRequest asSnapshotVersion() {
        this.snapshot = true;
        return this;
    }

    public Bump getBump() {
        return bump;
    }

    public String getIdentifiers() {
        return identifiers;
    }

    public String getSnapshotSuffix() {
        return snapshotSuffix;
    }

    public String getReleaseSuffix() {
        return releaseSuffix;
    }

    public boolean isSnapshot() {
        return snapshot;
    }

    public String getPatternWithoutVersionSuffix() {
        String pattern = "^.";

        if(!major.isEmpty()) {
            pattern = String.format("^%s", major);

            if(!minor.isEmpty()) {
                pattern = String.format("%s\\.%s", pattern, minor);

                if(!patch.isEmpty()) {
                    pattern = String.format("%s\\.%s", pattern, patch);
                }
            }
        }

        return pattern;
    }

    public String getPattern() {
        String pattern = getPatternWithoutVersionSuffix();

        if(!identifiers.isEmpty()) {
            pattern = String.format("%s-%s", pattern, identifiers);
        }

        return pattern;
    }

    public enum Bump {
        MAJOR(0), MINOR(1), PATCH(2);

        private int index;

        Bump(int index) {
            this.index = index;
        }

        public int getIndex() {
            return index;
        }
    }
}
