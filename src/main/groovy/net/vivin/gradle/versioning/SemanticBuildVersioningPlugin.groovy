package net.vivin.gradle.versioning

import net.vivin.gradle.versioning.tasks.BumpTask
import net.vivin.gradle.versioning.tasks.TagTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.tooling.BuildException

class SemanticBuildVersioningPlugin implements Plugin<Project> {

    private final Logger logger = Logging.getLogger("semantic-build-versioning")

    @Override
    void apply(Project project) {
        SemanticBuildVersion version = new SemanticBuildVersion(project)

        project.gradle.taskGraph.whenReady {
            VersionComponent bump = null;

            if (it.hasTask(project.tasks.release)) {
                version.snapshot = false
            }

            if (it.hasTask(project.tasks.newPreRelease)) {
                version.newPreRelease = true;
            }

            if (it.hasTask(project.tasks.promoteToRelease)) {
                if(version.newPreRelease) {
                    throw new BuildException("Cannot promote to a release version when also creating a new pre-release version", null)
                }

                version.promoteToRelease = true
            }

            if (it.hasTask(project.tasks.bumpPreRelease)) {
                if(version.newPreRelease) {
                    throw new BuildException("Cannot bump pre-release version when also creating a new pre-release version", null)
                }

                if(version.promoteToRelease) {
                    throw new BuildException("Cannot bump pre-release version when also promoting to a release version", null)
                }

                bump = VersionComponent.PRERELEASE
            }

            if (it.hasTask(project.tasks.bumpPatch)) {
                if(bump != null) {
                    throw new BuildException("Cannot bump multiple version-components at the same time", null)
                }

                if(version.promoteToRelease) {
                    throw new BuildException("Cannot bump patch-version when also promoting to a release version", null)
                }

                bump = VersionComponent.PATCH
            }

            if (it.hasTask(project.tasks.bumpMinor)) {
                if(bump != null) {
                    throw new BuildException("Cannot bump multiple version-components at the same time", null)
                }

                if(version.promoteToRelease) {
                    throw new BuildException("Cannot bump minor-version when also promoting to a release version", null)
                }

                bump = VersionComponent.MINOR
            }

            if (it.hasTask(project.tasks.bumpMajor)) {
                if(bump != null) {
                    throw new BuildException("Cannot bump multiple version-components at the same time", null)
                }

                if(version.promoteToRelease) {
                    throw new BuildException("Cannot bump major-version when also promoting to a release version", null)
                }

                bump = VersionComponent.MAJOR
            }

            if (it.hasTask(project.tasks.autobump)) {
                if(bump != null) {
                    throw new BuildException("Cannot explicitly bump a version-component when also autobumping", null)
                }

                if(version.promoteToRelease) {
                    throw new BuildException("Cannot explicitly promote to release when also autobumping", null)
                }

                if(version.newPreRelease) {
                    throw new BuildException("Cannot explicitly create a new pre-release version when also autobumping", null)
                }

                version.autobump = true
            }

            version.bump = bump
        }

        project.setVersion(version)
        project.task('promoteToRelease', group: "versioning")
        project.task('newPreRelease', group: "versioning")
        project.task('bumpPreRelease', type: BumpTask, group: "versioning")
        project.task('bumpPatch', type: BumpTask, group: "versioning")
        project.task('bumpMinor', type: BumpTask, group: "versioning")
        project.task('bumpMajor', type: BumpTask, group: "versioning")
        project.task('autobump', type: BumpTask, group: "versioning")
        project.task('tag', type: TagTask, group: "versioning") {
            semanticBuildVersion = version
        }
        project.task('tagAndPush', type: TagTask, group: "versioning") {
            push true
            semanticBuildVersion = version
        }

        project.task('printVersion') << {
            logger.quiet project.version as String
        }

        project.tasks.getByName('printVersion').mustRunAfter('promoteToRelease').mustRunAfter(project.tasks.withType(BumpTask))

        project.afterEvaluate {
            if(project.tasks.findByName('release') == null) {
                project.task('release') << {
                    logger.lifecycle("Releasing $project.name versioning $project.version")
                }
            }

            project.tasks.getByName('release').mustRunAfter('promoteToRelease').mustRunAfter(project.tasks.withType(BumpTask))
            project.tasks.getByName('tag').mustRunAfter('promoteToRelease').mustRunAfter(project.tasks.withType(BumpTask))
        }
    }
}
