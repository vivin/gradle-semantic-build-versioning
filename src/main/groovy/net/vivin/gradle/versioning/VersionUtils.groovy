package net.vivin.gradle.versioning

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.errors.GitAPIException
import org.eclipse.jgit.lib.Constants
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.gradle.tooling.BuildException

import java.util.regex.Pattern

/**
 * Created on 1/11/17 at 2:43 PM
 * @author vivin
 */
class VersionUtils {

    private static final String PRE_RELEASE_PART_REGEX = /(?!-)(?:(?:[1-9]\d*+|\p{Alpha}|[\p{Alnum}-]*[\p{Alpha}-][\p{Alnum}-]*+)(?<!-)(?:\.(?![$-])|$)?+)++/
    private static final Pattern PRE_RELEASE_PATTERN = ~/\d++\.\d++\.\d++-[\p{Alnum}.-]++/
    private static final Pattern VERSION_PATTERN = ~/.*\d++\.\d++\.\d++.*/
    private static final Pattern TAG_PREFIX_PATTERN = ~/^(?!\d++\.\d++\.\d)(?:.(?!\d++\.\d++\.\d))*./

    private final SemanticBuildVersion version
    private final Repository repository

    private tags
    private versions

    public VersionUtils(SemanticBuildVersion version, File workingDirectory) {
        this.version = version

        try {
            this.repository = new FileRepositoryBuilder()
                .setWorkTree(workingDirectory)
                .findGitDir(workingDirectory)
                .build()
        } catch(IOException e) {
            throw new BuildException("Unable to find Git repository: ${e.message}", e)
        }
    }

    public String determineVersion() {
        if(tags == null) {
            refresh()
        }

        if(version.newPreRelease && !version.config.preRelease) {
            throw new BuildException('Cannot create a new pre-release version if a preRelease configuration is not specified', null)
        }

        if((version.bump == VersionComponent.PRERELEASE) && !version.config.preRelease) {
            throw new BuildException('Cannot bump pre-release identifier if a preRelease configuration is not specified', null)
        }

        if(!version.snapshot && hasUncommittedChanges()) {
            throw new BuildException('Cannot create a release version when there are uncommitted changes', null)
        }

        if(!versions) {
            String determinedVersion = version.config.startingVersion
            if(version.newPreRelease) {
                // The starting version represents the next version to use if previous versions cannot be found. When
                // creating a new pre-release in this situation, it is not necessary to bump the starting version if the
                // component being bumped is the patch version, because that is supposed to be the next point-version.
                // However, if it is the major or minor version-component being bumped, then we have to bump the
                // starting version accordingly before appending the pre-release identifier. This way we don't end up
                // skipping a patch-version.

                if(version.bump != VersionComponent.PATCH) {
                    determinedVersion = incrementVersion determinedVersion
                }

                determinedVersion = "${determinedVersion}-${version.config.preRelease.startingVersion}"
            } else if(version.bump == VersionComponent.PRERELEASE) {
                throw new BuildException('Cannot bump pre-release because the latest version is not a pre-release version. To create a new pre-release version, use newPreRelease instead', null)
            }

            if(version.snapshot) {
                determinedVersion = "${determinedVersion}-${version.config.snapshotSuffix}"
            }

            return determinedVersion
        } else {
            String headTag = headTag
            if(hasUncommittedChanges() || headTag == null) {
                String versionFromTags = determineIncrementedVersionFromTags()
                if(version.newPreRelease) {
                    // We don't have to worry about the version already having a pre-release identifier because it is
                    // not possible to create a new pre-release version when also bumping the pre-release version.

                    versionFromTags = "${versionFromTags}-${version.config.preRelease.startingVersion}"
                }

                if(version.snapshot) {
                    versionFromTags = "${versionFromTags}-${version.config.snapshotSuffix}"
                }

                return versionFromTags
            } else {
                if(version.bump || version.newPreRelease || version.promoteToRelease) {
                    throw new BuildException('Cannot bump the version, create a new pre-release version, or promote a pre-release version because HEAD is currently pointing to a tag that identifies an existing version. To be able to create a new version, you must make changes', null)
                }

                version.snapshot = false
                return headTag - TAG_PREFIX_PATTERN
            }
        }
    }

    private String determineIncrementedVersionFromTags() {
        if(!version.bump) {
            if(version.promoteToRelease) {
                version.bump = VersionComponent.NONE
            } else if(!(latestVersion =~ PRE_RELEASE_PATTERN).find()) {
                version.bump = VersionComponent.PATCH
            } else {
                if(!version.config.preRelease) {
                    throw new BuildException("Cannot bump version because the latest version is '${latestVersion}', which contains preRelease identifiers. However, no preRelease configuration has been specified", null)
                }

                version.bump = VersionComponent.PRERELEASE
            }
        }

        return incrementVersion(latestVersion)
    }

    public String getLatestVersion() {
        if(tags == null) {
            refresh()
        }

        return versions?.find()
    }

    public boolean hasUncommittedChanges() {
        try {
            return new Git(repository)
                .status().call()
                .hasUncommittedChanges()
        } catch(GitAPIException e) {
            throw new BuildException("Unexpected error while determining repository status: ${e.message}", e)
        }
    }

    private String getHeadTag() {
        try {
            def headCommit = repository.resolve(Constants.HEAD)

            // return one of the non-filtered sem-ver tags that are pointing to HEAD
            return repository.tags
                .findAll { name, ref -> tags?.contains name }
                .collectEntries { [ it.key, repository.resolve("$it.value.name^0") ] }
                .findAll { it.value == headCommit }
                .collect { it.key }
                .find()
        } catch(IOException e) {
            throw new BuildException("Unexpected error while determining HEAD tag: ${e.message}", e)
        }
    }

    private String incrementVersion(String baseVersion) {
        String[] components = baseVersion.split(/[.-]/)
        SemanticVersion latest = new SemanticVersion(
            components[VersionComponent.MAJOR.index] as int,
            components[VersionComponent.MINOR.index] as int,
            components[VersionComponent.PATCH.index] as int
        )

        switch(version.bump) {
            case VersionComponent.MAJOR:
                latest.bumpMajor()
                break

            case VersionComponent.MINOR:
                latest.bumpMinor()
                break

            case VersionComponent.PATCH:
                latest.bumpPatch()
                break

            case VersionComponent.PRERELEASE:
                if(!(baseVersion =~ PRE_RELEASE_PATTERN).find()) {
                    throw new BuildException('Cannot bump pre-release because the latest version is not a pre-release version. To create a new pre-release version, use newPreRelease instead', null)
                } else {
                    String latestPreRelease = baseVersion - ~/^[^-]*-/
                    String nextPreRelease = version.config.preRelease.bump.dehydrate().call(latestPreRelease) as String

                    if(!isValidPreReleasePart(nextPreRelease)) {
                        throw new BuildException("Bumped pre-release version '${nextPreRelease}' is not a valid pre-release version. Identifiers must comprise only ASCII alphanumerics and hyphen, and numeric identifiers must not include leading zeroes", null)
                    }

                    return "${latest}-${nextPreRelease}"
                }
        }

        return latest as String
    }

    public void refresh() {
        Pattern tagPattern = version.config.tagPattern

        tags = repository.tags.keySet()
            .grep { (it =~ tagPattern).find() }
            .grep(VERSION_PATTERN)
            .grep { !version.config.matching || (it =~ version.config.matching.toPattern()).find() }
            .grep { !version.config.preRelease || (version.bump != VersionComponent.PRERELEASE) || !(it =~ PRE_RELEASE_PATTERN).find() || (it =~ version.config.preRelease.pattern).find() }

        versions = tags
            .collect { it - TAG_PREFIX_PATTERN }
            .unique()
            .toSorted new VersionComparator().reversed()
    }

    public static boolean isValidPreReleasePart(String preReleasePart) {
        preReleasePart ==~ PRE_RELEASE_PART_REGEX
    }
}
