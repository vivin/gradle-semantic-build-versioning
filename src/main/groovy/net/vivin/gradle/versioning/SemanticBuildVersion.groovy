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

            case { it.preReleasePattern }:
                highestAutobumpPattern = PRE_RELEASE
                break
        }

        VersionComponent autobump

        def patternMatcher = [:]

        if(config.autobump.majorPattern) {
            patternMatcher[MAJOR] = { it.any { it ==~ config.autobump.majorPattern } }
        }

        if(config.autobump.minorPattern) {
            if(highestAutobumpPattern == MAJOR) {
                patternMatcher[MINOR] = { (autobump != MINOR) && it.any { it ==~ config.autobump.minorPattern } }
            } else {
                patternMatcher[MINOR] = { it.any { it ==~ config.autobump.minorPattern } }
            }
        }

        if(config.autobump.patchPattern) {
            switch(highestAutobumpPattern) {
                case { config.autobump.minorPattern && (it == MAJOR) }:
                    patternMatcher[PATCH] = { ![MINOR, PATCH].contains(autobump) && it.any { it ==~ config.autobump.patchPattern } }
                    break

                case [MINOR, MAJOR]:
                    patternMatcher[PATCH] = { (autobump != PATCH) && it.any { it ==~ config.autobump.patchPattern } }
                    break

                default:
                    patternMatcher[PATCH] = { it.any { it ==~ config.autobump.patchPattern } }
                    break
            }
        }

        if(config.autobump.preReleasePattern) {
            switch(highestAutobumpPattern) {
                case { (config.autobump.minorPattern || config.autobump.patchPattern) && (it == MAJOR) }:
                    patternMatcher[PRE_RELEASE] = { ![MINOR, PATCH, PRE_RELEASE].contains(autobump) && it.any { it ==~ config.autobump.preReleasePattern } }
                    break

                case { config.autobump.patchPattern && (it == MINOR) }:
                    patternMatcher[PRE_RELEASE] = { ![PATCH, PRE_RELEASE].contains(autobump) && it.any { it ==~ config.autobump.preReleasePattern } }
                    break

                case [PATCH, MINOR, MAJOR]:
                    patternMatcher[PRE_RELEASE] = { (autobump != PRE_RELEASE) && it.any { it ==~ config.autobump.preReleasePattern } }
                    break

                default:
                    patternMatcher[PRE_RELEASE] = { it.any { it ==~ config.autobump.patchPattern } }
                    break
            }
        }

        patternMatcher = patternMatcher.toSorted { e1, e2 -> e2.key <=> e1.key }

        versionUtils.autobumpMessages.each {
            String[] lines = it.split(/\n/)

            if(!newPreRelease && config.autobump.newPreReleasePattern) {
                newPreRelease = lines.any { it ==~ config.autobump.newPreReleasePattern }
            }

            if(!promoteToRelease && config.autobump.promoteToReleasePattern) {
                promoteToRelease = lines.any { it ==~ config.autobump.promoteToReleasePattern }
            }

            // if manual bump is forced anyway, no commit message matching for bump components is necessary
            if(forceBump && bump) {
                return
            }

            // if autobump is already set to the highest value for which a pattern exists, do not do any more parsing
            if(autobump == highestAutobumpPattern) {
                return
            }

            autobump = patternMatcher.findResult(autobump) { it.value.isCase(lines) ? it.key : null }
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
