package net.vivin.gradle.plugins.version

import net.vivin.gradle.plugins.version.git.VersionComponent
import net.vivin.gradle.plugins.version.tasks.BumpTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging

/**
 * Created on 2/8/16 at 10:31 PM
 * @author vivin
 */
class SemanticBuildVersionPlugin implements Plugin<Project> {

    private final Logger logger = Logging.getLogger("semantic-build-versioning")

    @Override
    void apply(Project project) {
        SemanticBuildVersion version = new SemanticBuildVersion(project)

        if(project.tasks.findByName('release') == null) {
            project.task('release') << {
                logger.lifecycle("Releasing $project.name version $project.version")
            }
        }

        if(project.gradle.startParameter.taskNames.find { name ->
            name ==~ /:?release/
        }) {
            version.snapshot = false
        }

        if(project.gradle.startParameter.taskNames.find { name ->
            name ==~ /:?releaseVersion/
        }) {
            version.releaseVersion = true
        }

        if(project.gradle.startParameter.taskNames.find { name ->
            name ==~ /:?bumpIdentifier/
        }) {
            version.bump = VersionComponent.IDENTIFIER
        }

        if(project.gradle.startParameter.taskNames.find { name ->
            name ==~ /:?bumpPatch/
        }) {
            version.bump = VersionComponent.PATCH
        }

        if(project.gradle.startParameter.taskNames.find { name ->
            name ==~ /:?bumpMinor/
        }) {
            version.bump = VersionComponent.MINOR
        }

        if(project.gradle.startParameter.taskNames.find { name ->
            name ==~ /:?bumpMajor/
        }) {
            version.bump = VersionComponent.MAJOR
        }

        if(project.gradle.startParameter.taskNames.find { name ->
            name ==~ /:?autobump/
        }) {
            version.autobump = true
        }

        project.setVersion(version)
        project.task('releaseVersion', group: "versioning")
        project.task('bumpIdentifier', type: BumpTask, group: "versioning")
        project.task('bumpPatch', type: BumpTask, group: "versioning")
        project.task('bumpMinor', type: BumpTask, group: "versioning")
        project.task('bumpMajor', type: BumpTask, group: "versioning")
        project.task('autobump', type: BumpTask, group: "versioning")

        project.task('printVersion') << {
            println project.version
        }

        project.tasks.getByName('printVersion').mustRunAfter('releaseVersion').mustRunAfter(project.tasks.withType(BumpTask))
        project.tasks.getByName('release').mustRunAfter('releaseVersion').mustRunAfter(project.tasks.withType(BumpTask))
    }
}
