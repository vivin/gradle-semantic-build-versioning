package net.vivin.gradle.plugins.version

import net.vivin.gradle.plugins.version.git.VersioningRequest
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.tooling.BuildException

/**
 * Created on 2/8/16 at 10:31 PM
 * @author vivin
 */
class SemanticBuildVersionPlugin implements Plugin<Project> {

    private final Logger logger = Logging.getLogger("sembuildver")

    @Override
    void apply(Project project) {
        project.setVersion(new SemanticBuildVersion(project))

        if(project.tasks.getByName('release')) {
            project.tasks.getByName('release') << {
                logger.lifecycle("Releasing $project.name version $project.version")
            }
        } else {
            project.task('release') << {
                logger.lifecycle("Releasing $project.name version $project.version")
            }
        }

        project.task('bump') << {
            String semverComponent = "patch"
            if(project.hasProperty("semverComponent")) {
                semverComponent = project.semverComponent
            }

            if(semverComponent != "major" && semverComponent != "minor" && semverComponent != "patch") {
                throw new BuildException("${semverComponent} is not a valid version-component; it must be one of major, minor, or patch", null)
            }

            String matchingMajor = ""
            if(project.hasProperty("matchingMajor")) {
                matchingMajor = project.matchingMajor

                if(!matchingMajor.matches(/^\d+$/)) {
                    throw new BuildException("Matching major-version must be a number", null)
                }
            }

            String matchingMinor = ""
            if(project.hasProperty("matchingMinor")) {
                matchingMinor = project.matchingMinor

                if(!matchingMinor.matches(/^\d+$/)) {
                    throw new BuildException("Matching minor-version must be a number", null)
                }

                if(!matchingMajor) {
                    throw new BuildException("If matching minor-version is specified, then matching major-version must also be specified", null)
                }
            }

            String matchingPatch = ""
            if(project.hasProperty("matchingPatch")) {
                matchingPatch = project.matchingPatch

                if(!matchingPatch.matches(/^\d+$/)) {
                    throw new BuildException("Matching patch-version must be a number", null)
                }

                if(!matchingMajor) {
                    throw new BuildException("If matching patch-version is specified, then matching minor and major-version must also be specified", null)
                }

                if(!matchingMinor) {
                    throw new BuildException("If matching patch-version is specified, then matching minor and major-version must also be specified", null)
                }
            }

            project.version.bump = VersioningRequest.Bump.valueOf(semverComponent.toUpperCase())

            project.version.matchingMajor = matchingMajor
            project.version.matchingMinor = matchingMinor
            project.version.matchingPatch = matchingPatch
        }

        project.task('printVersion') << {
            println "$project.version"
        }

        project.gradle.taskGraph.whenReady { taskGraph ->
            project.version.release = taskGraph.hasTask(':release')
        }
    }
}
