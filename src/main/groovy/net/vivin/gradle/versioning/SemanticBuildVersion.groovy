package net.vivin.gradle.versioning

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.errors.NoHeadException
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.gradle.api.Project
import org.gradle.tooling.BuildException

class SemanticBuildVersion {
    Project project

    SemanticBuildVersionConfiguration config

    VersionComponent bump = null

    boolean promoteToRelease = false

    boolean snapshot = true

    boolean autobump = false

    boolean newPreRelease = false

    VersionUtils versionUtils = null

    SemanticBuildVersion(Project project) {
        this.project = project
        this.versionUtils = new VersionUtils(this, project.projectDir)
    }

    private void setVersionComponentUsingAutobumpConfiguration() {
        Repository repository = new FileRepositoryBuilder()
            .setWorkTree(project.projectDir)
            .findGitDir(project.projectDir)
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

        newPreRelease = lines.any { it ==~ config.autobump.newPreReleasePattern }

        switch(lines) {
            case { it.any { it ==~ config.autobump.majorPattern } }:
                bump = VersionComponent.MAJOR
                break

            case { it.any { it ==~ config.autobump.minorPattern } }:
                bump = VersionComponent.MINOR
                break

            case { it.any { it ==~ config.autobump.patchPattern } }:
                bump = VersionComponent.PATCH
                break

            case { it.any { it ==~ config.autobump.preReleasePattern } }:
                if(newPreRelease) {
                    throw new BuildException("Could not autobump because it is not possible to bump the pre-release version when also creating a new pre-release version", null)
                }
                bump = VersionComponent.PRERELEASE
                break

            case { it.any { it ==~ config.autobump.promoteToReleasePattern } }:
                if(newPreRelease) {
                    throw new BuildException("Could not autobump because it is not possible to promote to a release version when also creating a new pre-release version", null)
                }
                promoteToRelease = true
                break

            default:
                if(!newPreRelease) {
                    throw new BuildException("Could not autobump because the last commit message did not match the major (/${config.autobump.majorPattern}/), minor (/${config.autobump.minorPattern}/), patch (/${config.autobump.patchPattern}/), pre-release (/${config.autobump.preReleasePattern}/), or release (/${config.autobump.promoteToReleasePattern}/) patterns specified in the autobump configuration", null)
                }
                bump = VersionComponent.PATCH
                break
        }
    }

    String toString() {
        if(autobump) {
            setVersionComponentUsingAutobumpConfiguration()
        } else if(newPreRelease && bump == null) {
            bump = VersionComponent.PATCH
        }

        versionUtils.refresh()
        return versionUtils.determineVersion()
    }
}
