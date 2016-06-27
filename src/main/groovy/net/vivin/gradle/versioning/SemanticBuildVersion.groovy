package net.vivin.gradle.versioning

import net.vivin.gradle.versioning.git.VersionUtils
import net.vivin.gradle.versioning.git.VersionComponent
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.errors.NoHeadException
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.gradle.api.Project
import org.gradle.tooling.BuildException

import java.util.regex.Pattern

/**
 * Created on 2/8/16 at 10:02 PM
 * @author vivin
 */
class SemanticBuildVersion {
    Project project

    String startingVersion = "0.0.1"

    String tagPrefix = ""

    String snapshotSuffix = "SNAPSHOT"

    Pattern tagPattern = ~/.*/

    VersionsMatching versionsMatching = new VersionsMatching()
    PreRelease preReleaseConfiguration = null
    Autobump autobumpConfiguration = new Autobump()

    VersionComponent bump = null

    boolean promoteToRelease = false

    boolean snapshot = true

    boolean autobump = false

    VersionUtils versionUtils = null

    SemanticBuildVersion(Project project) {
        this.project = project
        this.versionUtils = new VersionUtils(this, project.getRootProject().projectDir.absolutePath)
    }

    void matching(Closure closure) {
        versionsMatching = (VersionsMatching) project.configure(new VersionsMatching(), closure)
        versionsMatching.validate()
    }

    void preRelease(Closure closure) {
        preReleaseConfiguration = (PreRelease) project.configure(new PreRelease(), closure)
        preReleaseConfiguration.validate()
    }

    void autobump(Closure closure) {
        autobumpConfiguration = (Autobump) project.configure(new Autobump(), closure)
        autobumpConfiguration.validate()
    }

    private void setVersionComponentUsingAutobumpConfiguration() {
        Repository repository = new FileRepositoryBuilder()
            .setWorkTree(new File(project.getRootProject().projectDir.absolutePath))
            .findGitDir(new File(project.getRootProject().projectDir.absolutePath))
            .build();

        String[] lines
        try {
            Git git = new Git(repository)
            Iterator<RevCommit> logIterator = git.log().call().iterator()

            // We don't need a hasNext check here because git.log() will fail if there are no prior commits, and we're
            // dealing with that in the catch block
            lines = logIterator.next().fullMessage.split(/\n/)
        } catch(NoHeadException e) {
            throw new BuildException("Could not autobump because there are no prior commits", e)
        }

        if(lines.find { it ==~ autobumpConfiguration.majorPattern }) {
            bump = VersionComponent.MAJOR
        } else if(lines.find { it ==~  autobumpConfiguration.minorPattern }) {
            bump = VersionComponent.MINOR
        } else if(lines.find { it ==~ autobumpConfiguration.patchPattern }) {
            bump = VersionComponent.PATCH
        } else if(lines.find { it ==~ autobumpConfiguration.preReleasePattern }) {
            bump = VersionComponent.PRERELEASE
        } else if(lines.find { it ==~ autobumpConfiguration.promoteToReleasePattern }) {
            promoteToRelease = true
        } else {
            throw new BuildException("Could not autobump because the last commit message did not match the major (/${autobumpConfiguration.majorPattern}/), minor (/${autobumpConfiguration.minorPattern}/), patch (/${autobumpConfiguration.patchPattern}/), pre-release (/${autobumpConfiguration.preReleasePattern}/), or release (/${autobumpConfiguration.promoteToReleasePattern}/) patterns specified in the autobump configuration", null)
        }
    }

    String toString() {
        if(!(startingVersion ==~ /^\d+\.\d+\.\d+(-[0-9A-Za-z-]+(\.[0-9A-Za-z-]+)+)?/)) {
            throw new BuildException("Provided starting version is not a valid semantic version", null)
        }

        tagPrefix = tagPrefix.trim()

        if(autobump) {
            setVersionComponentUsingAutobumpConfiguration()
        }

        versionUtils.refresh()
        return versionUtils.determineVersion()
    }
}
