package net.vivin.gradle.versioning

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.errors.GitAPIException
import org.eclipse.jgit.lib.Constants
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.revwalk.RevWalk
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
    private versionByTag

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

        if(version.newPreRelease && version.promoteToRelease) {
            throw new BuildException('Creating a new pre-release while also promoting a pre-release is not supported', null)
        }

        if(version.bump && version.promoteToRelease) {
            throw new BuildException('Bumping any component while also promoting a pre-release is not supported', null)
        }

        if((version.bump == VersionComponent.PRERELEASE) && version.newPreRelease) {
            throw new BuildException('Bumping pre-release component while also creating a new pre-release is not supported', null)
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

        String result

        if(!versionByTag) {
            // The starting version represents the next version to use if previous versions cannot be found. When
            // creating a new pre-release in this situation, it is not necessary to bump the starting version if the
            // component being bumped is the patch version, because that is supposed to be the next point-version.
            // However, if it is the major or minor version-component being bumped, then we have to bump the
            // starting version accordingly before appending the pre-release identifier. This way we don't end up
            // skipping a patch-version.
            result = incrementVersion version.config.startingVersion, true

            if(version.newPreRelease) {
                result = "${result}-${version.config.preRelease.startingVersion}"
            }
        } else {
            String headTag = getLatestTag(Constants.HEAD)
            if(hasUncommittedChanges() || headTag == null) {
                String versionFromTags = determineIncrementedVersionFromTags()
                if(version.newPreRelease) {
                    // We don't have to worry about the version already having a pre-release identifier because it is
                    // not possible to create a new pre-release version when also bumping the pre-release version.

                    versionFromTags = "${versionFromTags}-${version.config.preRelease.startingVersion}"
                }

                result = versionFromTags
            } else {
                if(version.bump || version.newPreRelease || version.promoteToRelease) {
                    throw new BuildException('Cannot bump the version, create a new pre-release version, or promote a pre-release version because HEAD is currently pointing to a tag that identifies an existing version. To be able to create a new version, you must make changes', null)
                }

                version.snapshot = false
                return headTag - TAG_PREFIX_PATTERN
            }
        }

        if(version.snapshot) {
            result = "$result-$version.config.snapshotSuffix"
        } else if(versionByTag.containsValue(result)) {
            throw new BuildException("Determined version '$result' already exists in the repository at '$repository.directory'.\nFix your bumping or manually create a tag with the intended version on the commit to be released.", null)
        } else if(!filterTags(["$version.config.tagPrefix$result"])) {
            throw new BuildException("Determined tag '$version.config.tagPrefix$result' is filtered out by configuration, this is not supported.\nFix your filter config, tag prefix config or bumping or manually create a tag with the intended version on the commit to be released.", null)
        }

        return result;
    }

    private String determineIncrementedVersionFromTags() {
        def latestVersion = latestVersion

        if(!version.bump) {
            if(version.promoteToRelease) {
                version.bump = VersionComponent.NONE
            } else if(!(latestVersion =~ PRE_RELEASE_PATTERN).find() || version.newPreRelease) {
                version.bump = VersionComponent.PATCH
            } else if(!version.config.preRelease) {
                throw new BuildException("Cannot bump version because the latest version is '${latestVersion}', which contains preRelease identifiers. However, no preRelease configuration has been specified", null)
            } else {
                version.bump = VersionComponent.PRERELEASE
            }
        }

        return incrementVersion(latestVersion)
    }

    public String getLatestVersion() {
        if(tags == null) {
            refresh()
        }

        if(!versionByTag) {
            return null
        } else {
            try {
                def revWalk = new RevWalk(repository)
                def candidateTags = []
                def toInvestigateForTags = []
                def alreadyInvestigated = []

                toInvestigateForTags.add(revWalk.parseCommit(repository.resolve(Constants.HEAD)))

                for(RevCommit investigatee = toInvestigateForTags.pop();
                    investigatee;
                    investigatee = toInvestigateForTags ? toInvestigateForTags.pop() : null) {

                    if(alreadyInvestigated.contains(investigatee)) {
                        continue
                    }
                    alreadyInvestigated << investigatee

                    String investigateeTag = getLatestTag(investigatee.name)
                    if(tags.contains(investigateeTag)) {
                        candidateTags.add investigateeTag
                    } else {
                        toInvestigateForTags.addAll revWalk.parseCommit(investigatee.id).parents
                    }
                }

                return candidateTags
                    .unique()
                    .collect { versionByTag."$it" }
                    .toSorted(new VersionComparator().reversed())
                    .find()
            } catch(IOException e) {
                throw new BuildException("Unexpected error while parsing HEAD commit: $e.message", e)
            }
        }
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

    /**
     * @return the latest sem-ver tag that is pointing to the given argument and not filtered
     */
    private String getLatestTag(String revstr) {
        try {
            def commit = repository.resolve(revstr)

            return repository.tags
                .findAll { name, ref -> tags?.contains name }
                .collectEntries { [it.key, repository.resolve("$it.value.name^{commit}")] }
                .findAll { it.value == commit }
                .collect { it.key }
                .toSorted(new VersionComparator().reversed())
                .find()
        } catch(IOException e) {
            throw new BuildException("Unexpected error while determining tag: ${e.message}", e)
        }
    }

    private String incrementVersion(String baseVersion, boolean onlyIfNecessary = false) {
        String[] components = baseVersion.split(/[.-]/, 4)
        SemanticVersion latest = new SemanticVersion(
            components[VersionComponent.MAJOR.index] as int,
            components[VersionComponent.MINOR.index] as int,
            components[VersionComponent.PATCH.index] as int
        )

        switch(version.bump) {
            case { (it == VersionComponent.MAJOR) && (!onlyIfNecessary || latest.minor || latest.patch) }:
                latest.bumpMajor()
                break

            case { (it == VersionComponent.MINOR) && (!onlyIfNecessary || latest.patch) }:
                latest.bumpMinor()
                break

            case { (it == VersionComponent.PATCH) && !onlyIfNecessary }:
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
        tags = filterTags(repository.tags.keySet())
        versionByTag = tags.collectEntries { [it, it - TAG_PREFIX_PATTERN] }
    }

    private filterTags(tags) {
        Pattern tagPattern = version.config.tagPattern

        tags.grep { (it =~ tagPattern).find() }
            .grep(VERSION_PATTERN)
            .grep { !version.config.matching || (it =~ version.config.matching.toPattern()).find() }
            .grep { !version.config.preRelease || (version.bump != VersionComponent.PRERELEASE) || !(it =~ PRE_RELEASE_PATTERN).find() || (it =~ version.config.preRelease.pattern).find() }
    }

    public static boolean isValidPreReleasePart(String preReleasePart) {
        preReleasePart ==~ PRE_RELEASE_PART_REGEX
    }
}
