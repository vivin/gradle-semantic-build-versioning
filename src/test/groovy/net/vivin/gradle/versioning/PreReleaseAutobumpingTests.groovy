package net.vivin.gradle.versioning

import org.gradle.tooling.BuildException
import org.testng.annotations.Test

import static org.testng.Assert.assertEquals

class PreReleaseAutobumpingTests extends TestNGRepositoryTestCase {

    @Test(expectedExceptions = BuildException)
    void testBumpingPreReleaseVersionWithoutPreReleaseConfigurationCausesBuildToFail() {
        testRepository
            .makeChanges()
            .commit("This is a message\n[pre-release]")

        SemanticBuildVersion version = (SemanticBuildVersion) project.getVersion()
        release(version)
        autobump(version)

        version.toString()
    }

    @Test(expectedExceptions = BuildException)
    void testAutobumpedPreReleaseVersionWithoutPriorPreReleaseVersionCausesBuildToFail() {
        testRepository
            .makeChanges()
            .commit("This is a message\n[pre-release]")

        SemanticBuildVersion version = (SemanticBuildVersion) project.getVersion()
        version.preRelease {
            startingVersion = "alpha.0"
            bump { String s ->
                s
            }
        }
        release(version)
        autobump(version)

        project.version.toString()
    }

    @Test(expectedExceptions = BuildException)
    void testAutobumpedPreReleaseVersionWithPriorNonPreReleaseVersionCausesBuildToFail() {
        testRepository
            .commitAndTag("0.2.0")
            .makeChanges()
            .commit("This is a message\n[pre-release]")

        SemanticBuildVersion version = (SemanticBuildVersion) project.getVersion()
        version.preRelease {
            startingVersion = "alpha.0"
            bump { String s ->
                s
            }
        }
        release(version)
        autobump(version)

        project.version.toString()
    }

    @Test
    void testAutobumpedPreReleaseVersionWithPriorPreReleaseVersionIsBumpedPreReleaseVersion() {
        testRepository
            .commitAndTag("0.2.0")
            .commitAndTag("0.2.1-alpha.0")
            .makeChanges()
            .commit("This is a message\n[pre-release]")

        SemanticBuildVersion version = (SemanticBuildVersion) project.getVersion()
        version.preRelease {
            startingVersion = "alpha.0"
            bump { String s ->
                String[] parts = s.split("\\.")
                return "${parts[0]}.${Integer.parseInt(parts[1]) + 1}"
            }
        }
        release(version)
        autobump(version)

        assertEquals(project.version.toString(), "0.2.1-alpha.1")
    }

    @Test(expectedExceptions = BuildException)
    void testAutobumpedPreReleaseVersionWithInvalidBumpedPreReleaseVersionCausesBuildToFail() {
        testRepository
            .commitAndTag("0.2.0")
            .commitAndTag("0.2.1-alpha.0")
            .makeChanges()
            .commit("This is a message\n[pre-release]")

        SemanticBuildVersion version = (SemanticBuildVersion) project.getVersion()
        version.preRelease {
            startingVersion = "alpha.0"
            bump { String s ->
                String[] parts = s.split("\\.")
                return "${parts[0]}.${Integer.parseInt(parts[1]) + 1}.00^1"
            }
        }
        release(version)
        autobump(version)

        version.toString()
    }

    @Test(expectedExceptions = BuildException)
    void testWithPatternAutobumpedPreReleaseVersionWithoutPriorPreReleaseVersionCausesBuildToFail() {
        testRepository
            .makeChanges()
            .commit("This is a message\n[pre-release]")

        SemanticBuildVersion version = (SemanticBuildVersion) project.getVersion()
        version.preRelease {
            startingVersion = "beta.0"
            pattern = ~/beta/
            bump { String s ->
                s
            }
        }
        release(version)
        autobump(version)

        project.version.toString()
    }

    @Test(expectedExceptions = BuildException)
    void testWithPatternAutobumpedPreReleaseVersionWithPriorNonPreReleaseVersionCausesBuildToFail() {
        testRepository
            .commitAndTag("0.2.0")
            .makeChanges()
            .commit("This is a message\n[pre-release]")

        SemanticBuildVersion version = (SemanticBuildVersion) project.getVersion()
        version.preRelease {
            startingVersion = "beta.0"
            pattern = ~/beta/
            bump { String s ->
                s
            }
        }
        release(version)
        autobump(version)

        project.version.toString()
    }

    @Test(expectedExceptions = BuildException)
    void testWithPatternAutobumpedPreReleaseVersionWithNonMatchingPriorPreReleaseVersionCausesBuildToFail() {
        testRepository
            .commitAndTag("0.2.0")
            .commitAndTag("0.2.1-alpha.0")
            .makeChanges()
            .commit("This is a message\n[pre-release]")

        SemanticBuildVersion version = (SemanticBuildVersion) project.getVersion()
        version.preRelease {
            startingVersion = "beta.0"
            pattern = ~/beta/
            bump { String s ->
                String[] parts = s.split("\\.")
                return "${parts[0]}.${Integer.parseInt(parts[1]) + 1}"
            }
        }
        release(version)
        autobump(version)

        project.version.toString()
    }

    @Test
    void testWithPatternAutobumpedPreReleaseVersionWithPriorPreReleaseVersionIsBumpedPreReleaseVersion() {
        testRepository
            .commitAndTag("0.2.0")
            .commitAndTag("0.2.1-alpha.0")
            .commitAndTag("0.2.1-beta.0")
            .makeChanges()
            .commit("This is a message\n[pre-release]")

        SemanticBuildVersion version = (SemanticBuildVersion) project.getVersion()
        version.preRelease {
            startingVersion = "beta.0"
            pattern = ~/beta/
            bump { String s ->
                String[] parts = s.split("\\.")
                return "${parts[0]}.${Integer.parseInt(parts[1]) + 1}"
            }
        }
        release(version)
        autobump(version)

        assertEquals(project.version.toString(), "0.2.1-beta.1")
    }

    @Test
    void testPromotingPreReleaseVersion() {
        testRepository
            .commitAndTag("0.2.0")
            .commitAndTag("0.2.1-alpha.0")
            .commitAndTag("0.2.1-beta.0")
            .makeChanges()
            .commit("This is a message\n[promote]")

        SemanticBuildVersion version = (SemanticBuildVersion) project.getVersion()
        version.preRelease {
            startingVersion = "beta.0"
            pattern = ~/beta/
            bump { String s ->
                String[] parts = s.split("\\.")
                return "${parts[0]}.${Integer.parseInt(parts[1]) + 1}"
            }
        }
        release(version)
        autobump(version)
        promote(version)

        assertEquals(project.version.toString(), "0.2.1")
    }
}
