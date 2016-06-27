package net.vivin.gradle.versioning

import org.testng.annotations.Test

import static org.testng.Assert.assertEquals

class VersionSortingTests extends TestNGRepositoryTestCase {

    @Test
    void testLatestVersion() {
        testRepository
            .commitAndTag("0.0.1")
            .commitAndTag("0.0.2")
            .commitAndTag("0.1.0")
            .commitAndTag("0.1.1")
            .commitAndTag("1.0.0")
            .commitAndTag("2.0.0")
            .commitAndTag("3.0.0")

        SemanticBuildVersion version = (SemanticBuildVersion) project.getVersion()
        assertEquals(version.versionUtils.latestVersion, "3.0.0")
    }

    @Test
    void testLatestVersionIsNonPreReleaseVersion() {
        testRepository
            .commitAndTag("0.0.1")
            .commitAndTag("0.0.1-alpha")

        SemanticBuildVersion version = (SemanticBuildVersion) project.getVersion()
        assertEquals(version.versionUtils.latestVersion, "0.0.1")
    }

    @Test
    void testLatestVersionIsNumericallyHigherPreReleaseVersion() {
        testRepository
            .commitAndTag("0.0.1-2")
            .commitAndTag("0.0.1-3")

        SemanticBuildVersion version = (SemanticBuildVersion) project.getVersion()
        assertEquals(version.versionUtils.latestVersion, "0.0.1-3")
    }

    @Test
    void testLatestVersionIsNumericallyHigherPreReleaseVersionEvenWithMultipleIdentifiers() {
        testRepository
            .commitAndTag("0.0.1-alpha.2")
            .commitAndTag("0.0.1-alpha.3")

        SemanticBuildVersion version = (SemanticBuildVersion) project.getVersion()
        assertEquals(version.versionUtils.latestVersion, "0.0.1-alpha.3")
    }

    @Test
    void testLatestVersionIsLexicallyHigherPreReleaseVersion() {
        testRepository
            .commitAndTag("0.0.1-x")
            .commitAndTag("0.0.1-y")

        SemanticBuildVersion version = (SemanticBuildVersion) project.getVersion()
        assertEquals(version.versionUtils.latestVersion, "0.0.1-y")
    }

    @Test
    void testLatestVersionIsLexicallyHigherPreReleaseVersionEvenWithMultipleIdentifiers() {
        testRepository
            .commitAndTag("0.0.1-alpha.x")
            .commitAndTag("0.0.1-alpha.y")

        SemanticBuildVersion version = (SemanticBuildVersion) project.getVersion()
        assertEquals(version.versionUtils.latestVersion, "0.0.1-alpha.y")
    }

    @Test
    void testLatestVersionIsNonNumericPreReleaseVersion() {
        testRepository
            .commitAndTag("0.0.1-999")
            .commitAndTag("0.0.1-alpha")

        SemanticBuildVersion version = (SemanticBuildVersion) project.getVersion()
        assertEquals(version.versionUtils.latestVersion, "0.0.1-alpha")
    }

    @Test
    void testLatestVersionIsNonNumericPreReleaseVersionEvenWithMultipleIdentifiers() {
        testRepository
            .commitAndTag("0.0.1-alpha.999")
            .commitAndTag("0.0.1-alpha.beta")

        SemanticBuildVersion version = (SemanticBuildVersion) project.getVersion()
        assertEquals(version.versionUtils.latestVersion, "0.0.1-alpha.beta")
    }

    @Test
    void testLatestVersionIsVersionWithLargestSetOfPreReleaseFields() {
        testRepository
            .commitAndTag("0.0.1")
            .commitAndTag("0.0.2")
            .commitAndTag("0.1.0")
            .commitAndTag("0.1.1")
            .commitAndTag("1.0.0")
            .commitAndTag("2.0.0")
            .commitAndTag("3.0.0")
            .commitAndTag("3.0.1-alpha")
            .commitAndTag("3.0.1-alpha.0")
            .commitAndTag("3.0.1-alpha.1")
            .commitAndTag("3.0.1-alpha.beta.gamma")
            .commitAndTag("3.0.1-alpha.beta.gamma.delta")

        SemanticBuildVersion version = (SemanticBuildVersion) project.getVersion()
        assertEquals(version.versionUtils.latestVersion, "3.0.1-alpha.beta.gamma.delta")
    }

    @Test
    void testLatestVersionIsNotVersionWithLargestSetOfPreReleaseFields() {
        testRepository
            .commitAndTag("0.0.1")
            .commitAndTag("0.0.2")
            .commitAndTag("0.1.0")
            .commitAndTag("0.1.1")
            .commitAndTag("1.0.0")
            .commitAndTag("2.0.0")
            .commitAndTag("3.0.0")
            .commitAndTag("3.0.1-alpha")
            .commitAndTag("3.0.1-alpha.0")
            .commitAndTag("3.0.1-alpha.1")
            .commitAndTag("3.0.1-alpha.beta.gamma")
            .commitAndTag("3.0.1-alpha.beta.gamma.delta")
            .commitAndTag("3.0.1-beta")

        SemanticBuildVersion version = (SemanticBuildVersion) project.getVersion()
        assertEquals(version.versionUtils.latestVersion, "3.0.1-beta")
    }
}
