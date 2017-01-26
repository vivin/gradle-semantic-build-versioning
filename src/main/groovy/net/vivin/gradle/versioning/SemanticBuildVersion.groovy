package net.vivin.gradle.versioning

import org.gradle.api.Project
import org.gradle.tooling.BuildException

import static net.vivin.gradle.versioning.VersionComponent.MAJOR
import static net.vivin.gradle.versioning.VersionComponent.MINOR
import static net.vivin.gradle.versioning.VersionComponent.PATCH
import static net.vivin.gradle.versioning.VersionComponent.PRE_RELEASE

class SemanticBuildVersion {
    Project project

    SemanticBuildVersionConfiguration config

    VersionComponent bump = null

    boolean forceBump = false

    boolean promoteToRelease = false

    boolean snapshot = true

    boolean newPreRelease = false

    VersionUtils versionUtils = null

    String version

    SemanticBuildVersion(Project project) {
        this.project = project
        this.versionUtils = new VersionUtils(this, project.projectDir)
    }

    private void setVersionComponentUsingAutobumpConfiguration() {
        // if no autobump pattern is defined, no commit message matching is necessary
        if(!config.autobump.enabled) {
            return
        }

        VersionComponent highestAutobumpPattern
        switch(config.autobump) {
            case { it.majorPattern }:
                highestAutobumpPattern = MAJOR
                break

            case { it.minorPattern }:
                highestAutobumpPattern = MINOR
                break

            case { it.patchPattern }:
                highestAutobumpPattern = PATCH
                break
        }

        VersionComponent autobump

        def patternMatcher = [:]

        if(config.autobump.majorPattern) {
            patternMatcher[MAJOR] = { (it =~ config.autobump.majorPattern).find() }
        }

        if(config.autobump.minorPattern) {
            if(highestAutobumpPattern == MAJOR) {
                patternMatcher[MINOR] = { (autobump != MINOR) && (it =~ config.autobump.minorPattern).find() }
            } else {
                patternMatcher[MINOR] = { (it =~ config.autobump.minorPattern).find() }
            }
        }

        if(config.autobump.patchPattern) {
            switch(highestAutobumpPattern) {
                case { config.autobump.minorPattern && (it == MAJOR) }:
                    patternMatcher[PATCH] = { ![MINOR, PATCH].contains(autobump) && (it =~ config.autobump.patchPattern).find() }
                    break

                case [MINOR, MAJOR]:
                    patternMatcher[PATCH] = { (autobump != PATCH) && (it =~ config.autobump.patchPattern).find() }
                    break

                default:
                    patternMatcher[PATCH] = { (it =~ config.autobump.patchPattern).find() }
                    break
            }
        }

        patternMatcher = patternMatcher.toSorted { e1, e2 -> e2.key <=> e1.key }

        versionUtils.autobumpMessages.each { message ->
            if(!newPreRelease && config.autobump.newPreReleasePattern) {
                newPreRelease = (message =~ config.autobump.newPreReleasePattern).find()
            }

            if(!promoteToRelease && config.autobump.promoteToReleasePattern) {
                promoteToRelease = (message =~ config.autobump.promoteToReleasePattern).find()
            }

            // if manual bump is forced anyway, no commit message matching for bump components is necessary
            if(forceBump && bump) {
                return
            }

            // if autobump is already set to the highest value for which a pattern exists, do not do any more parsing
            if(autobump == highestAutobumpPattern) {
                return
            }

            autobump = patternMatcher.findResult(autobump) { it.value.isCase(message) ? it.key : null }
        }

        if(autobump) {
            if(!bump) {
                // if autobump is set and manual bump not, use autobump
                bump = autobump
            } else if(bump < autobump) {
                // if autobump and manual bump are set, but manual bump is less than autobump without force bump, throw exception
                throw new BuildException('You are trying to manually bump a version component with less precedence than the one specified by the commit message. If you are sure you want to do this, use "forceBump".', null)
            }
            // manual bump is at least autobump, use manual bump
        }
    }

    String toString() {
        if(!version) {
            setVersionComponentUsingAutobumpConfiguration()
            version = versionUtils.determineVersion()
            versionUtils.cleanup()
        }
        return version
    }
}
