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

import static java.util.Collections.reverseOrder

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

    private Set<String> tags
    private Map<String, String> versionByTag
    private List<String> autobumpMessages
    private String latestVersion

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
        prefillState()

        if(version.newPreRelease && version.promoteToRelease) {
            throw new BuildException('Creating a new pre-release while also promoting a pre-release is not supported', null)
        }

        if(version.bump && version.promoteToRelease) {
            throw new BuildException('Bumping any component while also promoting a pre-release is not supported', null)
        }

        if((version.bump == VersionComponent.PRE_RELEASE) && version.newPreRelease) {
            throw new BuildException('Bumping pre-release component while also creating a new pre-release is not supported', null)
        }

        if(version.newPreRelease && !version.config.preRelease) {
            throw new BuildException('Cannot create a new pre-release version if a preRelease configuration is not specified', null)
        }

        if((version.bump == VersionComponent.PRE_RELEASE) && !version.config.preRelease) {
            throw new BuildException('Cannot bump pre-release identifier if a preRelease configuration is not specified', null)
        }

        if(!version.snapshot && hasUncommittedChanges()) {
            throw new BuildException('Cannot create a release version when there are uncommitted changes', null)
        }

        String result

        if(!versionByTag) {
            // This means that we didn't find any tags (taking into account filtering as well) and so we have to use
            // the starting version.

            // We cannot always increment the starting version here. We have to make sure that doing so does not end
            // up skipping a version series or a point-release. The method we are calling here will ensure that we
            // bump only if it is necessary
            result = determineIncrementedVersionFromStartingVersion()

            if(version.newPreRelease) {
                result = "${result}-${version.config.preRelease.startingVersion}"
            }
        } else {
            String headTag = getLatestTagOnReference(Constants.HEAD)
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
            throw new BuildException("Determined version '$result' already exists on another commit in the repository at '$repository.directory'.\nCheck your configuration to ensure that you haven't forgotten to filter out certain tags or versions. You may also be bumping the wrong component; if so, bump the component that will give you the intended version, or manually create a tag with the intended version on the commit to be released.", null)
        } else if(!filterTags(["$version.config.tagPrefix$result"])) {
            throw new BuildException("Determined tag '$version.config.tagPrefix$result' is filtered out by your configuration; this is not supported.\nCheck your filtering and tag-prefix configuration. You may also be bumping the wrong component; if so, bump the component that will give you the intended version, or manually create a tag with the intended version on the commit to be released.", null)
        }

        return result;
    }

    private String determineIncrementedVersionFromStartingVersion() {
        if(version.bump == null) {
            version.bump = VersionComponent.PATCH
        }

        String[] components = version.config.startingVersion.split(/\./, 3)
        SemanticVersion startingVersion = new SemanticVersion(
            components[VersionComponent.MAJOR.index] as int,
            components[VersionComponent.MINOR.index] as int,
            components[VersionComponent.PATCH.index] as int
        )

        String latest = version.config.startingVersion
        switch(version.bump) {
            case { (it == VersionComponent.MAJOR) && (!startingVersion.major || startingVersion.minor || startingVersion.patch) }:
                latest = incrementVersion(latest)
                break

            case { (it == VersionComponent.MINOR) && ((!startingVersion.major && !startingVersion.minor) || startingVersion.patch) }:
                latest = incrementVersion(latest)
                break

            case { (it == VersionComponent.PATCH) && ((!startingVersion.major && !startingVersion.minor && !startingVersion.patch)) }:
                latest = incrementVersion(latest)
                break

            case VersionComponent.PRE_RELEASE:
                throw new BuildException('Cannot bump pre-release because the latest version is not a pre-release version. To create a new pre-release version, use newPreRelease instead', null) // starting version never contains pre-release identifiers
        }

        return latest
    }

    private String determineIncrementedVersionFromTags() {

        // If bump is not specified, we need to figure out what to bump
        if(!version.bump) {

            if(version.promoteToRelease) {
                // We are promoting a pre-release to release, so nothing to bump
                version.bump = VersionComponent.NONE
            } else if(!(latestVersion =~ PRE_RELEASE_PATTERN).find() || version.newPreRelease) {
                // If latest version is not a pre-release or if we are making a new pre-release, then bump patch
                version.bump = VersionComponent.PATCH
            } else if(!version.config.preRelease) {
                // Looks like the latest version is a pre-release version, but we can't bump it because we don't have
                // a config that tells us how to do it
                throw new BuildException("Cannot bump version because the latest version is '${latestVersion}', which contains pre-release identifiers. However, no preRelease configuration has been specified", null)
            } else {
                version.bump = VersionComponent.PRE_RELEASE
            }
        }

        return incrementVersion(latestVersion)
    }

    public String getLatestVersion() {
        prefillState()
        latestVersion
    }

    public String[] getAutobumpMessages() {
        prefillState()
        autobumpMessages ?: []
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
    private String getLatestTagOnReference(String revstr) {
        try {
            def commit = repository.resolve(revstr)

            return repository.tags
                .findAll { name, ref -> tags?.contains name }
                .collectEntries { [it.key, repository.resolve("$it.value.name^{commit}")] }
                .findAll { it.value == commit }
                .collect { it.key }
                .toSorted(reverseOrder(new VersionComparator()))
                .find()
        } catch(IOException e) {
            throw new BuildException("Unexpected error while determining tag: ${e.message}", e)
        }
    }

    private String incrementVersion(String baseVersion) {
        String[] components = baseVersion.split(/[.-]/, 4)
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

            case VersionComponent.PRE_RELEASE:
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

    private prefillState() {
        if(tags != null) {
            return
        }

        tags = filterTags(repository.tags.keySet())
        versionByTag = tags.collectEntries { [it, it - TAG_PREFIX_PATTERN] }

        try {
            def revWalk = new RevWalk(repository)

            def nearestAncestorTags = []
            def references = [] as Stack<RevCommit>
            def investigatedReferences = [] as Set<RevCommit>

            def headCommit = repository.resolve(Constants.HEAD)
            if(!headCommit) {
                return // If there is no HEAD, we are done
            }

            // While we are going through the commits, we will also collect the messages if autobump is enabled
            autobumpMessages = version.config.autobump.enabled ? [] : null

            // This is a depth-first traversal; references is the frontier set (stack)
            references.add(revWalk.parseCommit(headCommit))

            while(references) {
                RevCommit reference = references.pop()
                investigatedReferences << reference

                String tag = getLatestTagOnReference(reference.name)
                if(tags.contains(tag)) {
                    nearestAncestorTags << tag
                } else {
                    RevCommit commit = revWalk.parseCommit(reference.id)
                    if(autobumpMessages != null) {
                        autobumpMessages << commit.fullMessage
                    }

                    references.addAll commit.parents.findAll { !investigatedReferences.contains(it) }
                }
            }

            latestVersion = nearestAncestorTags
                .unique()
                .collect { versionByTag."$it" }
                .toSorted(reverseOrder(new VersionComparator()))
                .find()

        } catch(IOException e) {
            throw new BuildException("Unexpected error while parsing HEAD commit: $e.message", e)
        }
    }

    private filterTags(tags) {
        Pattern tagPattern = version.config.tagPattern

        tags.grep { (it =~ tagPattern).find() }
            .grep(VERSION_PATTERN)
            .grep { !version.config.matching || (it =~ version.config.matching.toPattern()).find() }
            .grep { !version.config.preRelease || !(it =~ PRE_RELEASE_PATTERN).find() || (it =~ version.config.preRelease.pattern).find() }
    }

    public static boolean isValidPreReleasePart(String preReleasePart) {
        preReleasePart ==~ PRE_RELEASE_PART_REGEX
    }

    void cleanup() {
        tags = null
        versionByTag = null
        autobumpMessages = null
        latestVersion = null
    }
}
