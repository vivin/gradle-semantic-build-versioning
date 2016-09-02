package net.vivin.gradle.versioning;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevTag;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.gradle.tooling.BuildException;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class VersionUtils {

    private final SemanticBuildVersion version;
    private final Repository repository;

    private Set<String> tags = null;
    private SortedSet<String> versions = null;

    public VersionUtils(SemanticBuildVersion version, String workingDirectory) {
        this.version = version;

        try {
            this.repository = new FileRepositoryBuilder()
                .setWorkTree(new File(workingDirectory))
                .findGitDir(new File(workingDirectory))
                .build();
        } catch(IOException e) {
            throw new BuildException(String.format("Unable to find Git repository: %s", e.getMessage()), e);
        }
    }

    public void refresh() {
        try {
            retrieveTagInformation();
        } catch(GitAPIException e) {
            throw new BuildException(String.format("Unexpected error while retrieving tags: %s", e.getMessage()), e);
        }
    }

    public String determineVersion() {
        if(tags == null) {
            refresh();
        }

        if(version.isNewPreRelease() && version.getPreReleaseConfiguration() == null) {
            throw new BuildException("Cannot create a new pre-release version if a preRelease configuration is not specified", null);
        }

        if(version.getBump() == VersionComponent.PRERELEASE && version.getPreReleaseConfiguration() == null) {
            throw new BuildException("Cannot bump pre-release identifier if a preRelease configuration is not specified", null);
        }

        if(!version.isSnapshot() && hasUncommittedChanges()) {
            throw new BuildException("Cannot create a release version when there are uncommitted changes", null);
        }

        if(versions == null) {
            String determinedVersion = version.getStartingVersion();
            if(version.isNewPreRelease()) {

                // The starting version represents the next version to use if previous versions cannot be found. When
                // creating a new pre-release in this situation, it is not necessary to bump the starting version if the
                // component being bumped is the patch version, because that is supposed to be the next point-version.
                // However, if it is the major or minor version-component being bumped, then we have to bump the
                // starting version accordingly before appending the pre-release identifier. This way we don't end up
                // skipping a patch-version.

                if(version.getBump() != VersionComponent.PATCH) {
                    determinedVersion = incrementVersion(determinedVersion);
                }

                determinedVersion = String.format("%s-%s", determinedVersion, version.getPreReleaseConfiguration().getStartingVersion());
            } else if(version.getBump() == VersionComponent.PRERELEASE) {
                determinedVersion = String.format("%s-%s", determinedVersion, version.getPreReleaseConfiguration().getStartingVersion());
            }

            if(version.isSnapshot()) {
                determinedVersion = String.format("%s-%s", determinedVersion, version.getSnapshotSuffix());
            }

            return determinedVersion;
        } else {
            String headTag = getHeadTag();
            if(hasUncommittedChanges() || headTag == null) {
                String versionFromTags = determineIncrementedVersionFromTags();
                if(version.isNewPreRelease()) {
                    // We don't have to worry about the version already having a pre-release identifier because it is
                    // not possible to create a new pre-release version when also bumping the pre-release version.

                    versionFromTags = String.format("%s-%s", versionFromTags, version.getPreReleaseConfiguration().getStartingVersion());
                }

                return versionFromTags;
            } else {
                version.setSnapshot(false);
                return headTag.replaceFirst("^.*?(\\d+\\.\\d+\\.\\d+-?)", "$1");
            }
        }
    }

    private String determineIncrementedVersionFromTags() {
        String latestVersion = getLatestVersion();
        if(latestVersion == null) {
            latestVersion = version.getStartingVersion();
        } else if(version.getBump() == null) {
            if(version.isPromoteToRelease()) {
                version.setBump(VersionComponent.NONE);
            } else if(!latestVersion.contains("-")) {
                version.setBump(VersionComponent.PATCH);
            } else {
                if(version.getPreReleaseConfiguration() == null) {
                    throw new BuildException("Cannot bump version because the latest version is %s, which contains pre-release identifiers. However, no pre-release configuration has been specified", null);
                }

                version.setBump(VersionComponent.PRERELEASE);
            }
        }

        return incrementVersion(latestVersion);
    }

    public String getLatestVersion() {
        if(tags == null) {
            refresh();
        }

        if(versions == null || versions.isEmpty()) {
            return null;
        } else {
            return versions.first();
        }
    }

    public boolean hasUncommittedChanges() {
        try {
            return new Git(repository)
                .status().call()
                .hasUncommittedChanges();
        } catch(GitAPIException e) {
            throw new BuildException(String.format("Unexpected error while determining repository status: %s", e.getMessage()), e);
        }
    }

    private String getHeadTag() {
        try {
            String tag = new Git(repository)
                .describe()
                .setTarget(repository.resolve(Constants.HEAD))
                .call();

            return tags == null || !tags.contains(tag) ? null : tag;
        } catch(GitAPIException | IOException e) {
            throw new BuildException(String.format("Unexpected error while determining HEAD tag: %s", e.getMessage()), e);
        }
    }

    private String incrementVersion(String latestVersion) {
        String incrementedVersion;
        String[] components = latestVersion.split("[\\.-]");
        SemanticVersion latest = new SemanticVersion(
            Integer.parseInt(components[VersionComponent.MAJOR.getIndex()]),
            Integer.parseInt(components[VersionComponent.MINOR.getIndex()]),
            Integer.parseInt(components[VersionComponent.PATCH.getIndex()])
        );

        VersionComponent bump = version.getBump();
        if(bump == VersionComponent.MAJOR || bump == VersionComponent.MINOR || bump == VersionComponent.PATCH) {
            if(bump == VersionComponent.MAJOR) {
                latest.bumpMajor();
            } else if(bump == VersionComponent.MINOR) {
                latest.bumpMinor();
            } else {
                latest.bumpPatch();
            }

            incrementedVersion = latest.toString();
        } else if(bump == VersionComponent.PRERELEASE) {
            if(!latestVersion.contains("-")) {
                latest.bumpPatch();
                incrementedVersion = String.format("%s-%s", latest, version.getPreReleaseConfiguration().getStartingVersion());
            } else {
                String latestPreRelease = latestVersion.substring(latestVersion.indexOf("-") + 1);
                String nextPreRelease = version.getPreReleaseConfiguration().getBump().call(latestPreRelease).toString();

                List<String> preReleaseVersionComponents = Arrays.asList(nextPreRelease.split("\\."));
                preReleaseVersionComponents = preReleaseVersionComponents.stream().filter(component ->
                    !Pattern.compile("^[0-9A-Za-z-]+$").matcher(component).find() ||
                        Pattern.compile("^\\d+$").matcher(component).find() && Pattern.compile("^0\\d+$").matcher(component).find()
                ).collect(Collectors.toList());
                if(preReleaseVersionComponents.size() > 0) {
                    throw new BuildException(String.format("Bumped pre-release version %s is not a valid pre-release version. Identifiers must comprise only ASCII alphanumerics and hyphen, and numeric identifiers must not include leading zeroes", nextPreRelease), null);
                }

                incrementedVersion = String.format("%s-%s", latest, nextPreRelease);
            }
        } else {
            incrementedVersion = latest.toString();
        }

        if(version.isSnapshot()) {
            incrementedVersion = String.format("%s-%s", incrementedVersion, version.getSnapshotSuffix());
        }

        return incrementedVersion;
    }

    private void retrieveTagInformation() throws GitAPIException {
        Pattern tagPattern = version.getTagPattern();

        RevWalk walk = new RevWalk(repository);
        Git git = new Git(repository);

        //git.fetch().setTagOpt(TagOpt.AUTO_FOLLOW).call(); //Fetch all tags first
        List<Ref> tagRefs = git.tagList().call();
        if(!tagRefs.isEmpty()) {
            tags = tagRefs.stream()
                .map(tagRef -> parseTag(walk, tagRef.getObjectId()).getTagName())
                .filter(tagName -> tagPattern.matcher(tagName).find())
                .filter(tagName -> version.getVersionsMatching().toPattern().matcher(tagName).find())
                .filter(tagName -> (version.getPreReleaseConfiguration() == null || version.getBump() != VersionComponent.PRERELEASE || !tagName.contains("-")) || version.getPreReleaseConfiguration().getPattern().matcher(tagName).find())
                .collect(Collectors.toSet());

            versions = tags.stream()
                .map(tagName -> tagName.replaceFirst("^.*?(\\d+\\.\\d+\\.\\d+-?)", "$1"))
                .collect(
                    Collectors.toCollection(() -> new TreeSet<>(this::compareVersions))
                );
        }
    }

    private int compareVersions(String a, String b) {
        String[] aVersionComponents = a.replaceAll("^(\\d+\\.\\d+\\.\\d+).*$", "$1").split("\\.");
        String[] bVersionComponents = b.replaceAll("^(\\d+\\.\\d+\\.\\d+).*$", "$1").split("\\.");

        int aNumericValue = 0;
        int bNumericValue = 0;

        if (!aVersionComponents[VersionComponent.MAJOR.getIndex()].equals(bVersionComponents[VersionComponent.MAJOR.getIndex()])) {
            aNumericValue = Integer.parseInt(aVersionComponents[VersionComponent.MAJOR.getIndex()]);
            bNumericValue = Integer.parseInt(bVersionComponents[VersionComponent.MAJOR.getIndex()]);

        } else if (!aVersionComponents[VersionComponent.MINOR.getIndex()].equals(bVersionComponents[VersionComponent.MINOR.getIndex()])) {
            aNumericValue = Integer.parseInt(aVersionComponents[VersionComponent.MINOR.getIndex()]);
            bNumericValue = Integer.parseInt(bVersionComponents[VersionComponent.MINOR.getIndex()]);

        } else if (!aVersionComponents[VersionComponent.PATCH.getIndex()].equals(bVersionComponents[VersionComponent.PATCH.getIndex()])) {
            aNumericValue = Integer.parseInt(aVersionComponents[VersionComponent.PATCH.getIndex()]);
            bNumericValue = Integer.parseInt(bVersionComponents[VersionComponent.PATCH.getIndex()]);

        } else if (a.contains("-") && b.contains("-")) {
            String aPreReleaseVersion = a.replaceFirst("\\d+\\.\\d+\\.\\d+", "").replaceAll("-(.*)$", "$1");
            String bPreReleaseVersion = b.replaceFirst("\\d+\\.\\d+\\.\\d+", "").replaceAll("-(.*)$", "$1");

            String[] aPreReleaseComponents = aPreReleaseVersion.split("\\.");
            String[] bPreReleaseComponents = bPreReleaseVersion.split("\\.");

            int i = 0;
            boolean bSmallerComponentSize = false;
            boolean matching = true;
            while (i < aPreReleaseComponents.length && !bSmallerComponentSize && matching) {
                bSmallerComponentSize = (i >= bPreReleaseComponents.length);
                if (!bSmallerComponentSize) {
                    matching = aPreReleaseComponents[i].equals(bPreReleaseComponents[i]);
                }

                i++;
            }

            if (matching) {
                aNumericValue = aPreReleaseComponents.length;
                bNumericValue = bPreReleaseComponents.length;
            } else {
                String aNonMatchingComponent = aPreReleaseComponents[i - 1];
                String bNonMatchingComponent = bPreReleaseComponents[i - 1];

                boolean aNumericComponent = Pattern.compile("^\\d+$").matcher(aNonMatchingComponent).matches();
                boolean bNumericComponent = Pattern.compile("^\\d+$").matcher(bNonMatchingComponent).matches();

                if (aNumericComponent && bNumericComponent) {
                    aNumericValue = Integer.parseInt(aNonMatchingComponent);
                    bNumericValue = Integer.parseInt(bNonMatchingComponent);
                } else if (!aNumericComponent && !bNumericComponent) {
                    aNumericValue = aNonMatchingComponent.compareTo(bNonMatchingComponent);
                    bNumericValue = -1 * aNumericValue;
                } else {
                    aNumericValue = aNumericComponent ? 0 : 1;
                    bNumericValue = bNumericComponent ? 0 : 1;
                }
            }
        } else if (a.contains("-") || b.contains("-")) {
            aNumericValue = a.contains("-") ? 0 : 1;
            bNumericValue = b.contains("-") ? 0 : 1;
        }

        return bNumericValue - aNumericValue;
    }

    private RevTag parseTag(RevWalk walk, ObjectId id) {
        try {
            return walk.parseTag(id);
        } catch(IOException e) {
            throw new BuildException(String.format("Unexpected error while retrieving tag information: %s", e.getMessage()), e);
        }
    }
}
