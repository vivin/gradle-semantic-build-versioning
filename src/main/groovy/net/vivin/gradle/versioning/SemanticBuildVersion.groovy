package net.vivin.gradle.versioning

import org.gradle.api.Project
import org.gradle.tooling.BuildException

import static net.vivin.gradle.versioning.VersionComponent.MAJOR
import static net.vivin.gradle.versioning.VersionComponent.MINOR
import static net.vivin.gradle.versioning.VersionComponent.PATCH

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

        // Keeps track of the highest-precedence autobump pattern that has been defined in the autobump configuration.
        // Since all patterns need not be specified, and any can be set to null, we need to figure out which pattern
        // corresponds to the highest-precedence version. For example, if only minorPattern and majorPattern are
        // non-null, then highestAutobumpPatternComponent is set to MAJOR.

        VersionComponent highestAutobumpPatternComponent = null
        switch(config.autobump) {
            case { it.majorPattern }:
                highestAutobumpPatternComponent = MAJOR
                break

            case { it.minorPattern }:
                highestAutobumpPatternComponent = MINOR
                break

            case { it.patchPattern }:
                highestAutobumpPatternComponent = PATCH
                break
        }

        // Checking commit messages and matching them against regexes is a costly process. So we will do our best to
        // get rid of unnecessary checks so that we can determine the appropriate component to bump very quickly. To do
        // this, we set up matchers for each of the corresponding version-components, that try to match the commit
        // message against the regex only if the current componentToAutobump is a component with lower precedence
        // than the corresponding component for the matcher.
        //
        // Whether the highestAutobumpPatternComponent is already bumped is checked separately and is not needed to be
        // checked inside the individual matchers.

        VersionComponent componentToAutobump = null

        def patternMatcher = [:] as Map<VersionComponent, Closure>

        // If majorPattern is defined, we set the corresponding matcher to check if the commit message matches
        // majorPattern
        if(config.autobump.majorPattern) {
            patternMatcher[MAJOR] = { (it =~ config.autobump.majorPattern).find() }
        }

        // If minorPattern is specified, we have two cases
        if(config.autobump.minorPattern) {

            // If we are also checking whether there is a match against majorPattern, we set the corresponding matcher
            // to check if the commit message matches minorPattern, but only if we are not already bumping the minor
            // version (gets rid of unnecessary check).
            //
            // Otherwise, we just check to see if it matches minorPattern
            if(highestAutobumpPatternComponent == MAJOR) {
                patternMatcher[MINOR] = { componentToAutobump != MINOR && (it =~ config.autobump.minorPattern).find() }
            } else {
                patternMatcher[MINOR] = { (it =~ config.autobump.minorPattern).find() }
            }
        }

        // If patchPattern is specified, we have the following cases
        if(config.autobump.patchPattern) {
            switch(highestAutobumpPatternComponent) {
                // If we are checking for the major and minor pattern as well, we set the corresponding matcher to check
                // if the commit message matches patchPattern only if we are not already bumping the minor or patch
                // version
                case { (it == MAJOR) && config.autobump.minorPattern }:
                    patternMatcher[PATCH] = { ![MINOR, PATCH].contains(componentToAutobump) && (it =~ config.autobump.patchPattern).find() }
                    break

                // If we are checking for either the major or minor pattern as well, but not both (that is the previous case branch),
                // we set the corresponding matcher to check if the commit message matches patchPattern only
                // if we are not already bumping the patch version.
                case [MAJOR, MINOR]:
                    patternMatcher[PATCH] = { componentToAutobump != PATCH && (it =~ config.autobump.patchPattern).find() }
                    break

                // We set the corresponding matcher to check if the commit message matches patchPattern
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

            // If manual bump is forced anyway, no commit message matching for bump components is necessary
            if(forceBump && bump) {
                return
            }

            // If autobump is already set to the highest value for which a pattern exists, do not do any more matching.
            if(componentToAutobump == highestAutobumpPatternComponent) {
                return
            }

            componentToAutobump = patternMatcher.findResult(componentToAutobump) { it.value.isCase(message) ? it.key : null }
        }

        if(componentToAutobump) {
            if(!bump) {
                // If autobump is set and manual bump not, use autobump
                bump = componentToAutobump
            } else if(bump < componentToAutobump) {
                // If autobump and manual bump are set, but manual bump is less than autobump without force bump, throw exception
                throw new BuildException('You are trying to manually bump a version component with less precedence than the one specified by the commit message. If you are sure you want to do this, use "forceBump".', null)
            }
            // Manual bump is at least autobump, use manual bump
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
