package net.vivin.gradle.versioning

import org.gradle.tooling.BuildException
import org.testng.annotations.Test

import static org.testng.Assert.assertFalse

class VersionUtilsTests extends TestNGRepositoryTestCase {
    @Test(expectedExceptions = BuildException,
          expectedExceptionsMessageRegExp = /The version '0\.1' does not contain a semantic versioning part, please check your tag matching settings/)
    void testNonSemVerTagsFailTheBuildWithAProperMessage() {
        testRepository.commitAndTag('0.0.1')
            .makeChanges()
            .commitAndTag('0.1')

        SemanticBuildVersion version = (SemanticBuildVersion) project.getVersion()
        version.tagPattern = ~/.*/

        version.versionUtils.determineVersion()
    }

    @Test
    void testTaggedVersionIsRecognizedAsNonSnapshot() {
        testRepository.commitAndTag('0.0.1')

        SemanticBuildVersion version = (SemanticBuildVersion) project.getVersion()

        version.versionUtils.determineVersion()
        assertFalse(version.snapshot, 'Tagged version should not be snapshot version')
    }
}
