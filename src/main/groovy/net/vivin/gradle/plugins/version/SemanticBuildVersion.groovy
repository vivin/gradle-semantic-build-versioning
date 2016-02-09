package net.vivin.gradle.plugins.version

import net.vivin.gradle.plugins.version.git.TagVersion
import net.vivin.gradle.plugins.version.git.VersioningRequest
import org.gradle.api.Project
import org.gradle.tooling.BuildException

/**
 * Created on 2/8/16 at 10:02 PM
 * @author vivin
 */
class SemanticBuildVersion {
    Project project

    String identifiers = ""

    String snapshotSuffix = "SNAPSHOT"
    String releaseSuffix = ""

    String matchingMajor = ""
    String matchingMinor = ""
    String matchingPatch = ""

    String version = null

    VersioningRequest.Bump bump = VersioningRequest.Bump.PATCH

    boolean release = false

    SemanticBuildVersion(Project project) {
        this.project = project
    }

    void configure(Map<String, ?> map) {
        if(map.identifiers && !map.identifiers.matches(/^[0-9A-Za-z-]+(\\.[0-9A-Za-z-])*$/)) {
            throw new BuildException("Identifiers must comprise only ASCII alphanumerics and hyphen", null)
        }

        this.identifiers = map.identifiers ? map.identifiers : ""

        this.snapshotSuffix = map.snapshotSuffix ? map.snapshotSuffix : "SNAPSHOT"
        this.releaseSuffix = map.releaseSuffix ? map.releaseSuffix : ""
    }

    String toString() {
        TagVersion version = new TagVersion(project.getRootProject().projectDir.absolutePath)

        VersioningRequest request
        if(matchingMajor && matchingMinor && matchingPatch) {
            request = new VersioningRequest(matchingMajor, matchingMinor, matchingPatch, bump)
        } else if(matchingMajor && matchingMinor) {
            request = new VersioningRequest(matchingMajor, matchingMinor, bump)
        } else if(matchingMajor) {
            request = new VersioningRequest(matchingMajor, bump)
        } else {
            request = new VersioningRequest(bump)
        }

        request.withIdentifiers(identifiers)
            .withSnapshotSuffix(snapshotSuffix)
            .withReleaseSuffix(releaseSuffix)

        if(!release) {
            request.asSnapshotVersion()
        }

        return version.getNextVersionNumber(request)
    }
}
