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

    boolean forceBump = false

    boolean promoteToRelease = false

    boolean snapshot = true

    boolean newPreRelease = false

    VersionUtils versionUtils = null

    SemanticBuildVersion(Project project) {
        this.project = project
        this.versionUtils = new VersionUtils(this, project.projectDir)
    }

    private void setVersionComponentUsingAutobumpConfiguration() {
        if(!config.autobump.enabled) {
            return
        }

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
            // Could not autobump because there are no prior commits
            return
        }

        if(config.autobump.newPreReleasePattern && lines.any { it ==~ config.autobump.newPreReleasePattern }) {
            newPreRelease = true
        }

        if(config.autobump.promoteToReleasePattern && lines.any { it ==~ config.autobump.promoteToReleasePattern }) {
            promoteToRelease = true
        }

        VersionComponent autobump = null
        switch(lines) {
            case { config.autobump.majorPattern && it.any { it ==~ config.autobump.majorPattern } }:
                autobump = VersionComponent.MAJOR
                break

            case { config.autobump.minorPattern && it.any { it ==~ config.autobump.minorPattern } }:
                autobump = VersionComponent.MINOR
                break

            case { config.autobump.patchPattern && it.any { it ==~ config.autobump.patchPattern } }:
                autobump = VersionComponent.PATCH
                break

            case { config.autobump.preReleasePattern && it.any { it ==~ config.autobump.preReleasePattern } }:
                autobump = VersionComponent.PRERELEASE
                break
        }

        if(autobump) {
            if(!bump) {
                // if autobump is set and manual bump not, use autobump
                bump = autobump
            } else if(!forceBump && (bump < autobump)) {
                // if autobump and manual bump are set, but manual bump is less than autobump without force bump, throw exception
                throw new BuildException('You are trying to manually bump a version component with less precedence than the one specified by the commit message. If you are sure you want to do this, use "forceBump".', null)
            }
            // either forceBump is set or manual bump is at least autobump, use manual bump
        }
    }

    String toString() {
        setVersionComponentUsingAutobumpConfiguration()
        versionUtils.refresh()
        return versionUtils.determineVersion()
    }
}
