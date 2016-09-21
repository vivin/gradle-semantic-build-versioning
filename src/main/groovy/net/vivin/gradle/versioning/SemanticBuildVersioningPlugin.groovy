package net.vivin.gradle.versioning

import net.vivin.gradle.versioning.tasks.TagTask
import org.gradle.api.Plugin
import org.gradle.api.initialization.Settings
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.tooling.BuildException

class SemanticBuildVersioningPlugin implements Plugin<Settings> {
    private final Logger logger = Logging.getLogger('semantic-build-versioning')

    @Override
    void apply(Settings settings) {
        settings.gradle.allprojects { project ->
            def configFile = project.file('semantic-build-versioning.gradle')

            // if no config file is present, don't apply semantic versioning
            if(!configFile.file) {
                return
            }

            SemanticBuildVersion semanticBuildVersion = new SemanticBuildVersion(project)

            VersionComponent bump = null

            if(project.hasProperty('release')) {
                semanticBuildVersion.snapshot = false
            }

            if(project.hasProperty('newPreRelease')) {
                semanticBuildVersion.newPreRelease = true
            }

            if(project.hasProperty('promoteToRelease')) {
                if(semanticBuildVersion.newPreRelease) {
                    throw new BuildException('Cannot promote to a release version when also creating a new pre-release version', null)
                }

                semanticBuildVersion.promoteToRelease = true
            }

            if(project.hasProperty('bumpPreRelease')) {
                if(semanticBuildVersion.newPreRelease) {
                    throw new BuildException('Cannot bump pre-release version when also creating a new pre-release version', null)
                }

                if(semanticBuildVersion.promoteToRelease) {
                    throw new BuildException('Cannot bump pre-release version when also promoting to a release version', null)
                }

                bump = VersionComponent.PRERELEASE
            }

            if(project.hasProperty('bumpPatch')) {
                if(bump != null) {
                    throw new BuildException('Cannot bump multiple version-components at the same time', null)
                }

                if(semanticBuildVersion.promoteToRelease) {
                    throw new BuildException('Cannot bump patch-version when also promoting to a release version', null)
                }

                bump = VersionComponent.PATCH
            }

            if(project.hasProperty('bumpMinor')) {
                if(bump != null) {
                    throw new BuildException('Cannot bump multiple version-components at the same time', null)
                }

                if(semanticBuildVersion.promoteToRelease) {
                    throw new BuildException('Cannot bump minor-version when also promoting to a release version', null)
                }

                bump = VersionComponent.MINOR
            }

            if(project.hasProperty('bumpMajor')) {
                if(bump != null) {
                    throw new BuildException('Cannot bump multiple version-components at the same time', null)
                }

                if(semanticBuildVersion.promoteToRelease) {
                    throw new BuildException('Cannot bump major-version when also promoting to a release version', null)
                }

                bump = VersionComponent.MAJOR
            }

            if(project.hasProperty('autobump')) {
                if(bump != null) {
                    throw new BuildException('Cannot explicitly bump a version-component when also autobumping', null)
                }

                if(semanticBuildVersion.promoteToRelease) {
                    throw new BuildException('Cannot explicitly promote to release when also autobumping', null)
                }

                if(semanticBuildVersion.newPreRelease) {
                    throw new BuildException('Cannot explicitly create a new pre-release version when also autobumping', null)
                }

                semanticBuildVersion.autobump = true
            }

            semanticBuildVersion.bump = bump
            semanticBuildVersion.config = new ConfigSlurper().parse(configFile.toURI().toURL())
            semanticBuildVersion.config.validate()

            project.version = semanticBuildVersion as String
            project.version.metaClass.snapshot = semanticBuildVersion.snapshot
            project.ext.hasUncommittedChanges = semanticBuildVersion.versionUtils.&hasUncommittedChanges

            project.task('tag', type: TagTask, group: 'versioning') {
                onlyIf { !project.gradle.taskGraph.hasTask(project.tasks.tagAndPush) }
                tagPrefix semanticBuildVersion.config.tagPrefix
            }

            project.task('tagAndPush', type: TagTask, group: 'versioning') {
                tagPrefix semanticBuildVersion.config.tagPrefix
                push true
            }

            project.task('printVersion') << {
                logger.quiet project.version as String
            }

            project.tasks.all {
                if(name == 'release') {
                    it << {
                        logger.lifecycle "Releasing '$project.name' with version '$project.version'"
                    }
                }
            }
        }
    }
}
