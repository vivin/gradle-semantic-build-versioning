package net.vivin.gradle.plugins.version

import net.vivin.gradle.plugins.version.git.TagVersion
import net.vivin.gradle.plugins.version.git.VersionComponent
import org.eclipse.jgit.api.Git
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

    String snapshotSuffix = "SNAPSHOT"

    Pattern tagPattern = ~/.*/

    VersionsMatching versionsMatching = new VersionsMatching()
    PreRelease preReleaseConfiguration = null
    Autobump autobumpConfiguration = new Autobump()

    VersionComponent bump = null

    boolean promoteToRelease = false

    boolean snapshot = true

    boolean autobump = false

    String version = null

    String lastCommitMessage = null

    SemanticBuildVersion(Project project) {
        this.project = project
    }

    void matching(Closure closure) {
        project.configure(versionsMatching, closure)
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
        if(lastCommitMessage == null) {
            Repository repository = new FileRepositoryBuilder()
                .setWorkTree(new File(project.getRootProject().projectDir.absolutePath))
                .findGitDir()
                .build();

            Git git = new Git(repository)
            Iterator<RevCommit> logIterator = git.log().call().iterator()
            if (!logIterator.hasNext()) {
                throw new BuildException("Could not autobump because there are no commits", null)
            }

            lastCommitMessage = logIterator.next().fullMessage

            println "last message:\n${lastCommitMessage}"
        }

        String[] lines = lastCommitMessage.split(/\n/)
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
        if(this.version == null) {
            if(autobump) {
                setVersionComponentUsingAutobumpConfiguration()
            }

            TagVersion version = new TagVersion(project.getRootProject().projectDir.absolutePath)
            this.version = version.getNextVersion(this)
        }

        return this.version
    }
}
