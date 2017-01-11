package net.vivin.gradle.versioning;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.gradle.tooling.BuildException;

import java.io.File;
import java.io.IOException;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toSet;
import static net.vivin.gradle.versioning.PreRelease.isValidPreReleasePart;
import static org.eclipse.jgit.lib.Constants.HEAD;

public class VersionUtils {
    private static final Pattern PRE_RELEASE_PATTERN = Pattern.compile("\\d++\\.\\d++\\.\\d++-[\\p{Alnum}.-]++");
    private static final Pattern VERSION_PATTERN = Pattern.compile("\\d++\\.\\d++\\.\\d++");

    private final SemanticBuildVersion version;
    private final Repository repository;

    private Set<String> tags = null;
    private SortedSet<String> versions = new TreeSet<>();

    public VersionUtils(SemanticBuildVersion version, File workingDirectory) {
        this.version = version;

        try {
            this.repository = new FileRepositoryBuilder()
                .setWorkTree(workingDirectory)
                .findGitDir(workingDirectory)
                .build();
        } catch(IOException e) {
            throw new BuildException(format("Unable to find Git repository: %s", e.getMessage()), e);
        }
    }

    public String determineVersion() {
        if(tags == null) {
            refresh();
        }

        SemanticBuildVersionConfiguration versionConfig = version.getConfig();
        if(version.isNewPreRelease() && (versionConfig.getPreRelease() == null)) {
            throw new BuildException("Cannot create a new pre-release version if a preRelease configuration is not specified", null);
        }

        if((version.getBump() == VersionComponent.PRERELEASE) && (versionConfig.getPreRelease() == null)) {
            throw new BuildException("Cannot bump pre-release identifier if a preRelease configuration is not specified", null);
        }

        if(!version.isSnapshot() && hasUncommittedChanges()) {
            throw new BuildException("Cannot create a release version when there are uncommitted changes", null);
        }

        if(versions.isEmpty()) {
            String determinedVersion = versionConfig.getStartingVersion();
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

                determinedVersion = format("%s-%s", determinedVersion, versionConfig.getPreRelease().getStartingVersion());
            } else if(version.getBump() == VersionComponent.PRERELEASE) {
                throw new BuildException("Cannot bump pre-release because the latest version is not a pre-release version. To create a new pre-release version, use newPreRelease instead", null);
            }

            if(version.isSnapshot()) {
                determinedVersion = format("%s-%s", determinedVersion, versionConfig.getSnapshotSuffix());
            }

            return determinedVersion;
        } else {
            String headTag = getHeadTag();
            if(hasUncommittedChanges() || headTag == null) {
                String versionFromTags = determineIncrementedVersionFromTags();
                if(version.isNewPreRelease()) {
                    // We don't have to worry about the version already having a pre-release identifier because it is
                    // not possible to create a new pre-release version when also bumping the pre-release version.

                    versionFromTags = format("%s-%s", versionFromTags, versionConfig.getPreRelease().getStartingVersion());
                }

                if(version.isSnapshot()) {
                    versionFromTags = format("%s-%s", versionFromTags, versionConfig.getSnapshotSuffix());
                }

                return versionFromTags;
            } else {
                if(version.getBump() != null || version.isNewPreRelease() || version.isPromoteToRelease()) {
                    throw new BuildException("Cannot bump the version, create a new pre-release version, or promote a pre-release version because HEAD is currently pointing to a tag that identifies an existing version. To be able to create a new version, you must make changes", null);
                }

                version.setSnapshot(false);
                return headTag.replaceFirst("^.*?(\\d++\\.\\d++\\.\\d)", "$1");
            }
        }
    }

    private String determineIncrementedVersionFromTags() {
        String latestVersion = getLatestVersion();

        if(version.getBump() == null) {
            if(version.isPromoteToRelease()) {
                version.setBump(VersionComponent.NONE);
            } else if(!PRE_RELEASE_PATTERN.matcher(latestVersion).find()) {
                version.setBump(VersionComponent.PATCH);
            } else {
                if(version.getConfig().getPreRelease() == null) {
                    throw new BuildException(format("Cannot bump version because the latest version is '%s', which contains preRelease identifiers. However, no preRelease configuration has been specified", latestVersion), null);
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

        if(versions.isEmpty()) {
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
            throw new BuildException(format("Unexpected error while determining repository status: %s", e.getMessage()), e);
        }
    }

    private String getHeadTag() {
        try {
            // return one of the sem-ver tags that are pointing to HEAD
            Set<String> headTags = repository.getTags().entrySet().stream()
                .collect(groupingBy(entry -> {
                        try {
                            return repository.resolve(entry.getValue().getName() + "^0");
                        } catch(IOException e) {
                            throw new BuildException(format("Unexpected error while determining HEAD tag: %s", e.getMessage()), e);
                        }
                    },
                    mapping(Entry::getKey, toSet())))
                .get(repository.resolve(HEAD));

            return headTags == null ? null : headTags.stream().filter(tag -> tags.contains(tag)).findAny().orElse(null);
        } catch(IOException e) {
            throw new BuildException(format("Unexpected error while determining HEAD tag: %s", e.getMessage()), e);
        }
    }

    private String incrementVersion(String baseVersion) {
        String[] components = baseVersion.split("[\\.-]");
        SemanticVersion latest = new SemanticVersion(
            Integer.parseInt(components[VersionComponent.MAJOR.getIndex()]),
            Integer.parseInt(components[VersionComponent.MINOR.getIndex()]),
            Integer.parseInt(components[VersionComponent.PATCH.getIndex()])
        );

        switch(version.getBump()) {
            case MAJOR:
                latest.bumpMajor();
                break;

            case MINOR:
                latest.bumpMinor();
                break;

            case PATCH:
                latest.bumpPatch();
                break;

            case PRERELEASE:
                if(!PRE_RELEASE_PATTERN.matcher(baseVersion).find()) {
                    throw new BuildException("Cannot bump pre-release because the latest version is not a pre-release version. To create a new pre-release version, use newPreRelease instead", null);
                } else {
                    String latestPreRelease = baseVersion.substring(baseVersion.indexOf("-") + 1);
                    String nextPreRelease = version.getConfig().getPreRelease().getBump().dehydrate().call(latestPreRelease).toString();

                    if(!isValidPreReleasePart(nextPreRelease)) {
                        throw new BuildException(format("Bumped pre-release version '%s' is not a valid pre-release version. Identifiers must comprise only ASCII alphanumerics and hyphen, and numeric identifiers must not include leading zeroes", nextPreRelease), null);
                    }

                    return format("%s-%s", latest, nextPreRelease);
                }
        }

        return latest.toString();
    }

    public void refresh() {
        SemanticBuildVersionConfiguration versionConfig = version.getConfig();
        Pattern tagPattern = versionConfig.getTagPattern();

        //git.fetch().setTagOpt(TagOpt.AUTO_FOLLOW).call(); //Fetch all tags first
        Set<String> tagNames = repository.getTags().keySet();
        if(tagNames.isEmpty()) {
            return;
        }

        tags = tagNames.stream()
            .filter(tagName -> tagPattern.matcher(tagName).find())
            .filter(tagName -> VERSION_PATTERN.matcher(tagName).find())
            .filter(tagName -> (versionConfig.getMatching() == null) || versionConfig.getMatching().toPattern().matcher(tagName).find())
            .filter(tagName -> (versionConfig.getPreRelease() == null) || (version.getBump() != VersionComponent.PRERELEASE) || !PRE_RELEASE_PATTERN.matcher(tagName).find() || versionConfig.getPreRelease().getPattern().matcher(tagName).find())
            .collect(Collectors.toSet());

        versions = tags.stream()
            .map(tagName -> tagName.replaceFirst("^.*?(\\d++\\.\\d++\\.\\d)", "$1"))
            .collect(toCollection(() -> new TreeSet<>(new VersionComparator().reversed())));
    }
}
